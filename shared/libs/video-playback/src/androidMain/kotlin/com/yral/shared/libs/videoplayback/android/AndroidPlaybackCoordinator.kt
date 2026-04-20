package com.yral.shared.libs.videoplayback.android

import android.content.Context
import android.os.HandlerThread
import android.os.Process
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.preload.DefaultPreloadManager
import androidx.media3.exoplayer.source.preload.PreloadException
import androidx.media3.exoplayer.source.preload.PreloadManagerListener
import androidx.media3.exoplayer.source.preload.TargetPreloadStatusControl
import com.yral.shared.libs.videoplayback.ContainerHint
import com.yral.shared.libs.videoplayback.CoordinatorDeps
import com.yral.shared.libs.videoplayback.MediaDescriptor
import com.yral.shared.libs.videoplayback.PlaybackCoordinator
import com.yral.shared.libs.videoplayback.PlaybackProgress
import com.yral.shared.libs.videoplayback.PlaybackProgressTicker
import com.yral.shared.libs.videoplayback.PreloadEventScheduler
import com.yral.shared.libs.videoplayback.PreloadPolicy
import com.yral.shared.libs.videoplayback.PreparedSlotScheduler
import com.yral.shared.libs.videoplayback.SlotActivationMode
import com.yral.shared.libs.videoplayback.VideoSurfaceHandle
import com.yral.shared.libs.videoplayback.cacheKey
import com.yral.shared.libs.videoplayback.planFeedAlignment
import com.yral.shared.libs.videoplayback.selectSlotActivationDecision
import com.yral.shared.libs.videoplayback.ui.AndroidVideoSurfaceHandle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(UnstableApi::class)
fun createAndroidPlaybackCoordinator(
    context: Context,
    deps: CoordinatorDeps = CoordinatorDeps(),
): PlaybackCoordinator = AndroidPlaybackCoordinator(context, deps)

@OptIn(UnstableApi::class)
@Suppress("TooManyFunctions")
private class AndroidPlaybackCoordinator(
    context: Context,
    private val deps: CoordinatorDeps,
) : PlaybackCoordinator {
    private val appContext = context.applicationContext
    private val reporter = deps.reporter
    private val policy = deps.policy

    @kotlin.OptIn(ExperimentalTime::class)
    private val nowMs: () -> Long = { Clock.System.now().toEpochMilliseconds() }

    private val cache = ShortformCacheProvider.acquire(appContext, policy)
    private val preloadStatusControl = DefaultTargetPreloadStatusControl(policy)

    // Own the preload thread so we can install an UncaughtExceptionHandler.
    // Media3 1.9.0 has a race condition in PreloadMediaSource where clear()
    // can null out internal state while an in-flight onPrepared callback is
    // queued on this thread, causing a checkNotNull() NPE. By owning the
    // thread we convert that fatal crash into a non-fatal log.
    private val preloadThread =
        HandlerThread("YralPreload", Process.THREAD_PRIORITY_AUDIO).apply {
            start()
            uncaughtExceptionHandler =
                Thread.UncaughtExceptionHandler { _, throwable ->
                    Log.e(
                        "AndroidPlaybackCoord",
                        "Non-fatal crash on preload thread (Media3 race condition)",
                        throwable,
                    )
                }
        }

    private val preloadManagerBuilder =
        DefaultPreloadManager
            .Builder(appContext, preloadStatusControl)
            .setPreloadLooper(preloadThread.looper)
            .setCache(cache)
            .setLoadControl(createLoadControl())

    private val preloadManager = preloadManagerBuilder.build()

    private val playerA = createPlayer(preloadManagerBuilder)
    private val playerB = if (policy.usePreparedNextPlayer) createPlayer(preloadManagerBuilder) else null
    private val slotA = PlayerSlot(playerA)
    private val slotB = playerB?.let { PlayerSlot(it) }
    private var activeSlot = slotA
    private var preparedSlot: PlayerSlot? = slotB

    private val surfaces = mutableMapOf<Int, VideoSurfaceHandle>()
    private var feed: List<MediaDescriptor> = emptyList()
    private var mediaItems: List<MediaItem> = emptyList()
    private var mediaIndexById: Map<String, Int> = emptyMap()

    private var released = false
    private var activeIndex: Int = -1
    private var predictedIndex: Int = -1
    private val preloadScheduler = PreloadEventScheduler(policy, reporter)
    private val preparedScheduler = PreparedSlotScheduler(policy, reporter)

    private val playStartMsById = mutableMapOf<String, Long>()
    private var firstFramePendingIndex: Int? = null
    private var pendingPreparedScheduleIndex: Int? = null
    private var pendingPreparedScheduleJob: Job? = null
    private var rebuffering = false
    private var stalling = false
    private var stallStartMs: Long = 0
    private var fullyBufferedReported = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val progressTicker =
        PlaybackProgressTicker(
            intervalMs = deps.progressTickIntervalMs,
            scope = scope,
            provider = { activeProgress() },
            onProgress = { progress ->
                reporter.playbackProgress(
                    progress.id,
                    progress.index,
                    progress.positionMs,
                    progress.durationMs,
                )
                checkFullyBuffered(progress)
            },
        )
    private val playerAListener = createSlotListener(slotA)
    private val playerBListener = slotB?.let(::createSlotListener)

    private val preloadListener =
        object : PreloadManagerListener {
            override fun onCompleted(mediaItem: MediaItem) {
                val index = mediaIndexById[mediaItem.mediaId] ?: return
                val descriptor = feed.getOrNull(index) ?: return
                val cacheKey = mediaItem.localConfiguration?.customCacheKey ?: mediaItem.mediaId
                val cachedBytes = cache.getCachedBytes(cacheKey, 0, C.LENGTH_UNSET.toLong()).coerceAtLeast(0)
                reporter.preloadCompleted(descriptor.id, index, cachedBytes, 0, cachedBytes > 0)
            }

            override fun onError(exception: PreloadException) {
                val mediaItem = exception.mediaItem
                val index = mediaIndexById[mediaItem.mediaId] ?: return
                val descriptor = feed.getOrNull(index) ?: return
                reporter.preloadCanceled(descriptor.id, index, "error")
            }
        }

    init {
        playerA.addListener(playerAListener)
        playerB?.let { player ->
            playerBListener?.let(player::addListener)
        }
        preloadManager.addListener(preloadListener)
        progressTicker.start()
    }

    @Suppress("LongMethod")
    override fun setFeed(items: List<MediaDescriptor>) {
        if (released) return
        val previousFeed = feed
        val previousIds = previousFeed.map { it.id }
        val currentIds = items.map { it.id }
        val alignment =
            planFeedAlignment(
                previousIds = previousIds,
                currentIds = currentIds,
                activeIndex = activeIndex,
                activeSlotIndex = activeSlot.mediaIndex,
                preparedSlotIndex = preparedSlot?.mediaIndex,
            )
        preloadScheduler.reset("feed_update") { feed.getOrNull(it)?.id }
        preparedScheduler.reset("feed_update") { feed.getOrNull(it)?.id }

        // Build new state
        val newMediaItems = items.map { buildMediaItem(it) }
        val newMediaIndexById = items.mapIndexed { index, d -> d.id to index }.toMap()

        // Compute position-aware diff: only keep items at the same index
        val oldIdAtIndex = mediaItems.withIndex().associate { (i, mi) -> mi.mediaId to i }
        val newIdAtIndex = newMediaItems.withIndex().associate { (i, mi) -> mi.mediaId to i }

        // An item is "stable" only if it exists in both feeds at the same index
        val stableIds =
            oldIdAtIndex.keys
                .intersect(newIdAtIndex.keys)
                .filter { id ->
                    oldIdAtIndex[id] == newIdAtIndex[id]
                }.toSet()

        // Remove items that are gone or moved
        val itemsToRemove = mediaItems.filter { it.mediaId !in stableIds }
        if (itemsToRemove.isNotEmpty()) {
            preloadManager.removeMediaItems(itemsToRemove)
        }

        // Add items that are new or moved (with correct rankings)
        val itemsToAdd = newMediaItems.filter { it.mediaId !in stableIds }
        val rankingsToAdd = itemsToAdd.map { newIdAtIndex[it.mediaId] ?: 0 }
        if (itemsToAdd.isNotEmpty()) {
            preloadManager.addMediaItems(itemsToAdd, rankingsToAdd)
        }

        feed = items
        mediaItems = newMediaItems
        mediaIndexById = newMediaIndexById

        alignment.invalidatePreparedIndex?.let { _ ->
            preparedSlot?.let { slot ->
                slot.player.playWhenReady = false
                detachSurface(slot)
                slot.mediaIndex = null
                slot.isReady = false
                slot.hasRenderedFirstFrame = false
                slot.warmOnFirstFrame = false
            }
        }

        if (alignment.clearPlaybackState) {
            activeIndex = -1
            predictedIndex = -1
            rebuffering = false
            stalling = false
            stallStartMs = 0
            fullyBufferedReported = false
            firstFramePendingIndex = null
            pendingPreparedScheduleIndex = null
            pendingPreparedScheduleJob?.cancel()
            pendingPreparedScheduleJob = null
            activeSlot.player.playWhenReady = false
            preparedSlot?.player?.playWhenReady = false
            detachSurface(activeSlot)
            activeSlot.mediaIndex = null
            activeSlot.isReady = false
            activeSlot.hasRenderedFirstFrame = false
            activeSlot.warmOnFirstFrame = false
            preparedSlot?.let { slot ->
                detachSurface(slot)
                slot.isReady = false
                slot.hasRenderedFirstFrame = false
                slot.warmOnFirstFrame = false
                slot.mediaIndex = null
            }
            return
        }

        alignment.nextActiveIndex?.let { targetIndex ->
            activeIndex = -1
            setActiveIndex(targetIndex)
        }
    }

    override fun appendFeed(items: List<MediaDescriptor>) {
        if (released) return
        if (items.isEmpty()) return
        val startIndex = feed.size
        feed = feed + items
        val appendedMediaItems =
            items.mapIndexed { offset, descriptor ->
                buildMediaItem(descriptor)
            }
        mediaItems = mediaItems + appendedMediaItems
        mediaIndexById = feed.mapIndexed { index, descriptor -> descriptor.id to index }.toMap()
        val ranking = (startIndex until feed.size).toList()
        preloadManager.addMediaItems(appendedMediaItems, ranking)

        if (activeIndex == -1 && feed.isNotEmpty()) {
            setActiveIndex(0)
        }
    }

    @Suppress("ReturnCount")
    override fun setActiveIndex(index: Int) {
        if (released) return
        if (index !in feed.indices) return
        if (index == activeIndex && activeSlot.mediaIndex == index) return

        if (rebuffering) {
            feed.getOrNull(activeIndex)?.let { item ->
                reporter.rebufferEnd(item.id, activeIndex, "navigation")
            }
            rebuffering = false
        }
        if (stalling) {
            feed.getOrNull(activeIndex)?.let { item ->
                val elapsed = nowMs() - stallStartMs
                reporter.stallEnd(item.id, activeIndex, elapsed)
            }
            stalling = false
            stallStartMs = 0
        }
        fullyBufferedReported = false
        pendingPreparedScheduleJob?.cancel()
        pendingPreparedScheduleJob = null
        activeIndex = index
        predictedIndex = index
        preloadStatusControl.currentPlayingIndex = index
        preloadStatusControl.predictedIndex = index
        preloadManager.setCurrentPlayingIndex(index)
        preloadManager.invalidate()

        preloadScheduler.update(index, feed.size) { feed.getOrNull(it)?.id }

        val item = feed[index]
        reporter.feedItemImpression(item.id, index)
        val activationDecision =
            selectSlotActivationDecision(
                requestedIndex = index,
                preparedIndex = preparedSlot?.mediaIndex,
                preparedReady = preparedSlot?.isHot == true,
            )
        reporter.playStartRequest(item.id, index, activationDecision.playStartReason)
        playStartMsById[item.id] = nowMs()
        firstFramePendingIndex = index
        pendingPreparedScheduleIndex = index

        when (activationDecision.mode) {
            SlotActivationMode.SwapPrepared -> swapSlots()
            SlotActivationMode.PrepareActive -> {
                preparedSlot?.takeIf { it.mediaIndex == index }?.let { prepared ->
                    stopPreparedWarmup(prepared)
                    detachSurface(prepared)
                }
                prepareSlot(activeSlot, index, playWhenReady = true)
            }
        }

        attachIfBound(activeSlot)
        activeSlot.player.playWhenReady = true
        if (maybeReportPendingFirstFrame(activeSlot)) {
            maybeSchedulePreparedSlot(index)
        }
    }

    @Suppress("ReturnCount")
    override fun setScrollHint(
        predictedIndex: Int,
        velocity: Float?,
    ) {
        if (released) return
        if (predictedIndex !in feed.indices) return
        if (predictedIndex == this.predictedIndex) return
        this.predictedIndex = predictedIndex
        preloadStatusControl.predictedIndex = predictedIndex
        preloadManager.invalidate()
        preloadScheduler.update(predictedIndex, feed.size) { feed.getOrNull(it)?.id }
    }

    override fun bindSurface(
        index: Int,
        surface: VideoSurfaceHandle,
    ) {
        if (released) return
        surfaces[index] = surface
        if (activeSlot.mediaIndex == index) {
            attachIfBound(activeSlot)
        } else if (preparedSlot?.mediaIndex == index) {
            preparedSlot?.let {
                attachIfBound(it)
                warmPreparedSlot(it)
            }
        }
    }

    override fun unbindSurface(
        index: Int,
        surfaceId: String,
    ) {
        if (released) return
        val handle = surfaces[index]
        if (handle?.id != surfaceId) return
        surfaces.remove(index)
        if (handle is AndroidVideoSurfaceHandle) {
            if (activeSlot.boundSurfaceIndex == index && handle.playerState.value == activeSlot.player) {
                handle.playerState.value = null
                activeSlot.boundSurfaceIndex = null
            }
            val prepared = preparedSlot
            if (prepared != null &&
                prepared.boundSurfaceIndex == index &&
                handle.playerState.value == prepared.player
            ) {
                handle.playerState.value = null
                prepared.boundSurfaceIndex = null
            }
        }
    }

    override fun onAppForeground() {
        if (released) return
        if (activeSlot.mediaIndex in feed.indices) {
            activeSlot.player.playWhenReady = true
        }
    }

    override fun onAppBackground() {
        if (released) return
        activeSlot.player.playWhenReady = false
        preparedSlot?.let(::stopPreparedWarmup)
    }

    override fun release() {
        if (released) return
        released = true
        preloadScheduler.reset("release") { feed.getOrNull(it)?.id }
        preparedScheduler.reset("release") { feed.getOrNull(it)?.id }
        progressTicker.stop()
        scope.cancel()
        preloadManager.removeListener(preloadListener)
        pendingPreparedScheduleJob?.cancel()
        pendingPreparedScheduleJob = null

        // Release preload manager FIRST. Its release() internally uses
        // releasePreloadMediaSource() (safe: atomically nullifies +
        // removeCallbacksAndMessages). Then posts looper quit, dropping
        // any remaining handler messages (stale onPrepared callbacks).
        //
        // Do NOT call removeMediaItems() or invalidate() before release().
        // invalidate() triggers clear() which has a race condition with
        // in-flight onPrepared callbacks (Media3 bug).
        preloadManager.release()
        preloadThread.quitSafely()

        playerA.release()
        playerB?.release()
    }

    private fun createSlotListener(slot: PlayerSlot): Player.Listener =
        object : Player.Listener {
            @Suppress("ReturnCount")
            override fun onMediaItemTransition(
                mediaItem: MediaItem?,
                reason: Int,
            ) {
                if (slot !== activeSlot) return
                if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) return
                val index = mediaItem?.mediaId?.let { mediaIndexById[it] } ?: slot.mediaIndex ?: return
                val item = feed.getOrNull(index) ?: return
                reporter.playbackEnded(item.id, index)
            }

            @Suppress("ReturnCount")
            override fun onPlaybackStateChanged(playbackState: Int) {
                slot.isReady = playbackState == Player.STATE_READY

                val index = slot.mediaIndex ?: return
                val item = feed.getOrNull(index) ?: return

                if (slot === preparedSlot) {
                    if (playbackState == Player.STATE_READY) {
                        preparedScheduler.markReady(
                            index = index,
                            nowMs = nowMs(),
                            idAt = { feed.getOrNull(it)?.id },
                        )
                    }
                    return
                }

                if (slot !== activeSlot) return

                if (playbackState == Player.STATE_BUFFERING && slot.player.playWhenReady) {
                    if (!rebuffering) {
                        rebuffering = true
                        reporter.rebufferStart(item.id, index, "buffering")
                    }
                }

                if (playbackState == Player.STATE_READY && rebuffering) {
                    rebuffering = false
                    reporter.rebufferEnd(item.id, index, "buffering")
                }

                if (playbackState == Player.STATE_ENDED) {
                    reporter.playbackEnded(item.id, index)
                }
            }

            @Suppress("ReturnCount")
            override fun onPlayerError(error: PlaybackException) {
                slot.isReady = false
                slot.hasRenderedFirstFrame = false
                slot.warmOnFirstFrame = false

                val index = slot.mediaIndex ?: return
                if (slot === preparedSlot) {
                    preparedScheduler.markError(index, { feed.getOrNull(it)?.id }, "error")
                    return
                }

                if (slot !== activeSlot) return

                val item = feed.getOrNull(index) ?: return
                reporter.playbackError(
                    id = item.id,
                    index = index,
                    category = error.errorCodeName,
                    code = error.errorCode,
                    message = error.message,
                )
            }

            @Suppress("ReturnCount")
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (slot !== activeSlot) return

                val index = slot.mediaIndex ?: return
                val item = feed.getOrNull(index) ?: return
                val player = slot.player
                val isNonBufferStall =
                    player.playWhenReady &&
                        player.playbackState != Player.STATE_BUFFERING &&
                        !rebuffering
                if (!isPlaying && isNonBufferStall) {
                    if (!stalling) {
                        stalling = true
                        stallStartMs = nowMs()
                        reporter.stallStart(item.id, index, "playback_suppressed")
                    }
                } else if (isPlaying && stalling) {
                    val elapsed = nowMs() - stallStartMs
                    stalling = false
                    stallStartMs = 0
                    reporter.stallEnd(item.id, index, elapsed)
                }
            }

            @Suppress("ReturnCount")
            override fun onRenderedFirstFrame() {
                slot.hasRenderedFirstFrame = true
                if (slot === preparedSlot) {
                    if (slot.warmOnFirstFrame) {
                        stopPreparedWarmup(slot)
                    }
                    return
                }
                if (slot !== activeSlot) return
                val index = slot.mediaIndex ?: return
                if (maybeReportPendingFirstFrame(slot)) {
                    maybeSchedulePreparedSlot(index)
                }
            }
        }

    private fun schedulePreparedSlot(activeIndex: Int) {
        val prepared = preparedSlot ?: return
        preparedScheduler.schedule(activeIndex, feed.size, { feed.getOrNull(it)?.id }) { nextIndex ->
            if (prepared.mediaIndex != nextIndex) {
                prepareSlot(prepared, nextIndex, playWhenReady = false)
                attachIfBound(prepared)
                warmPreparedSlot(prepared)
                preparedScheduler.setStartTime(nowMs())
            }
        }
    }

    @Suppress("ReturnCount")
    private fun checkFullyBuffered(progress: PlaybackProgress) {
        if (fullyBufferedReported) return
        val player = activeSlot.player
        val duration = player.duration
        if (duration == C.TIME_UNSET || duration <= 0L) return
        if (player.bufferedPosition >= duration - BUFFER_TOLERANCE_MS) {
            fullyBufferedReported = true
            val startMs = playStartMsById[progress.id] ?: return
            reporter.videoFullyBuffered(progress.id, progress.index, nowMs() - startMs)
        }
    }

    @Suppress("ReturnCount")
    private fun activeProgress(): PlaybackProgress? {
        val index = activeSlot.mediaIndex ?: return null
        val item = feed.getOrNull(index) ?: return null
        val player = activeSlot.player
        if (!player.isPlaying) return null
        val durationMs = player.duration
        if (durationMs == C.TIME_UNSET || durationMs <= 0L) return null
        val positionMs = player.currentPosition.coerceAtLeast(0L)
        return PlaybackProgress(
            id = item.id,
            index = index,
            positionMs = positionMs,
            durationMs = durationMs,
        )
    }

    private fun swapSlots() {
        val prepared = preparedSlot ?: return
        val previousActive = activeSlot
        previousActive.player.playWhenReady = false
        previousActive.player.volume = PREPARED_PLAYER_VOLUME
        previousActive.isReady = false
        previousActive.hasRenderedFirstFrame = false
        previousActive.warmOnFirstFrame = false
        activeSlot = prepared
        preparedSlot = previousActive
        activeSlot.player.volume = ACTIVE_PLAYER_VOLUME
        activeSlot.warmOnFirstFrame = false
        preparedScheduler.clearOnSwap()
    }

    private fun prepareSlot(
        slot: PlayerSlot,
        index: Int,
        playWhenReady: Boolean,
    ) {
        val mediaItem = mediaItems.getOrNull(index) ?: return
        slot.mediaIndex = index
        slot.isReady = false
        slot.hasRenderedFirstFrame = false
        slot.warmOnFirstFrame = false
        slot.player.volume = if (playWhenReady) ACTIVE_PLAYER_VOLUME else PREPARED_PLAYER_VOLUME
        val mediaSource = preloadManager.getMediaSource(mediaItem)
        if (mediaSource != null) {
            slot.player.setMediaSource(mediaSource)
        } else {
            // Preloaded source not ready yet — let the player create its own source.
            slot.player.setMediaItem(mediaItem)
        }
        slot.player.prepare()
        slot.player.playWhenReady = playWhenReady
    }

    @Suppress("ReturnCount")
    private fun warmPreparedSlot(slot: PlayerSlot) {
        if (slot !== preparedSlot) return
        val mediaIndex = slot.mediaIndex ?: return
        if (slot.boundSurfaceIndex != mediaIndex) return
        if (slot.hasRenderedFirstFrame || slot.warmOnFirstFrame) return

        slot.warmOnFirstFrame = true
        slot.player.volume = PREPARED_PLAYER_VOLUME
        slot.player.playWhenReady = true
    }

    private fun stopPreparedWarmup(slot: PlayerSlot) {
        slot.warmOnFirstFrame = false
        slot.player.playWhenReady = false
        slot.player.volume = PREPARED_PLAYER_VOLUME
    }

    @Suppress("ReturnCount")
    private fun maybeReportPendingFirstFrame(slot: PlayerSlot): Boolean {
        if (!slot.hasRenderedFirstFrame) return false

        val index = slot.mediaIndex ?: return false
        val item = feed.getOrNull(index) ?: return false
        if (firstFramePendingIndex != index) return false

        firstFramePendingIndex = null
        val start = playStartMsById[item.id] ?: return false
        val elapsed = nowMs() - start
        reporter.firstFrameRendered(item.id, index)
        reporter.timeToFirstFrame(item.id, index, elapsed)
        return true
    }

    private fun maybeSchedulePreparedSlot(index: Int) {
        if (pendingPreparedScheduleIndex != index) return
        pendingPreparedScheduleJob?.cancel()
        pendingPreparedScheduleJob =
            scope.launch {
                delay(PREPARED_SLOT_SCHEDULE_DELAY_MS)
                if (pendingPreparedScheduleIndex != index) return@launch
                pendingPreparedScheduleIndex = null
                pendingPreparedScheduleJob = null
                schedulePreparedSlot(index)
            }
    }

    private fun attachIfBound(slot: PlayerSlot) {
        val targetIndex = slot.mediaIndex ?: return
        val targetHandle = surfaces[targetIndex] as? AndroidVideoSurfaceHandle ?: return
        val previousSurfaceIndex = slot.boundSurfaceIndex
        if (targetHandle.playerState.value != slot.player) {
            targetHandle.playerState.value = slot.player
        }
        if (previousSurfaceIndex != null && previousSurfaceIndex != targetIndex) {
            val previousHandle = surfaces[previousSurfaceIndex] as? AndroidVideoSurfaceHandle
            val previousPlayerState = previousHandle?.playerState
            if (previousPlayerState?.value == slot.player) {
                previousPlayerState.value = null
            }
        }
        slot.boundSurfaceIndex = targetIndex
    }

    private fun detachSurface(slot: PlayerSlot) {
        val index = slot.boundSurfaceIndex ?: return
        val handle = surfaces[index] as? AndroidVideoSurfaceHandle ?: return
        if (handle.playerState.value == slot.player) {
            handle.playerState.value = null
        }
        slot.boundSurfaceIndex = null
    }

    private fun buildMediaItem(descriptor: MediaDescriptor): MediaItem {
        val builder =
            MediaItem
                .Builder()
                .setUri(descriptor.uri)
                .setMediaId(descriptor.id)
                .setCustomCacheKey(descriptor.cacheKey())

        when (descriptor.containerHint) {
            ContainerHint.MP4 -> builder.setMimeType(MimeTypes.APPLICATION_MP4)
            ContainerHint.HLS -> builder.setMimeType(MimeTypes.APPLICATION_M3U8)
            ContainerHint.DASH -> builder.setMimeType(MimeTypes.APPLICATION_MPD)
            ContainerHint.UNKNOWN -> Unit
        }

        return builder.build()
    }

    private fun createPlayer(preloadManagerBuilder: DefaultPreloadManager.Builder): ExoPlayer {
        val audioAttributes =
            AudioAttributes
                .Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build()
        return preloadManagerBuilder.buildExoPlayer().apply {
            setAudioAttributes(audioAttributes, true)
            setHandleAudioBecomingNoisy(true)
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    private fun createLoadControl(): DefaultLoadControl =
        DefaultLoadControl
            .Builder()
            .setBufferDurationsMs(
                MIN_BUFFER_MS,
                MAX_BUFFER_MS,
                BUFFER_FOR_PLAYBACK_MS,
                BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
            ).setPrioritizeTimeOverSizeThresholds(true)
            .build()

    private object ShortformCacheProvider {
        private val lock = Any()
        private var cache: SimpleCache? = null
        private var databaseProvider: StandaloneDatabaseProvider? = null
        private var refCount = 0

        @Volatile
        private var isReleasing = false

        fun acquire(
            context: Context,
            policy: PreloadPolicy,
        ): SimpleCache {
            synchronized(lock) {
                val existing = cache
                if (existing != null && !isReleasing) {
                    refCount++
                    return existing
                }
                val cacheDir = File(context.cacheDir, "video-playback-cache")
                val evictor = LeastRecentlyUsedCacheEvictor(policy.cacheMaxBytes)
                val provider =
                    databaseProvider ?: StandaloneDatabaseProvider(context).also {
                        databaseProvider = it
                    }
                val created = SimpleCache(cacheDir, evictor, provider)
                cache = created
                refCount = 1
                isReleasing = false
                return created
            }
        }

        fun release() {
            synchronized(lock) {
                if (refCount > 0) {
                    refCount--
                }
                if (refCount == 0 && !isReleasing) {
                    isReleasing = true
                    val cacheToRelease = cache
                    cache = null // Clear reference first to prevent new operations
                    databaseProvider?.close()
                    databaseProvider = null

                    // Release cache - may throw if background threads are still accessing it
                    // This is expected in race conditions with Media3's async download cancellation
                    try {
                        cacheToRelease?.release()
                    } catch (
                        @Suppress("SwallowedException") e: IllegalStateException,
                    ) {
                        // Cache may be in use by background download threads that haven't
                        // been cancelled yet. This is a known race condition in Media3 when
                        // removeMediaItems() is called (which is async) and release() is called
                        // immediately after. The cache will be cleaned up on next app start.
                        // Swallow the exception to prevent crashes.
                    } finally {
                        isReleasing = false
                    }
                }
            }
        }
    }

    private companion object {
        private const val PREPARED_SLOT_SCHEDULE_DELAY_MS = 150L
        private const val ACTIVE_PLAYER_VOLUME = 1F
        private const val PREPARED_PLAYER_VOLUME = 0F
        private const val MIN_BUFFER_MS = 5_000
        private const val MAX_BUFFER_MS = 20_000
        private const val BUFFER_FOR_PLAYBACK_MS = 500
        private const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS =
            DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
        private const val BUFFER_TOLERANCE_MS = 500L
    }

    private data class PlayerSlot(
        val player: ExoPlayer,
        var mediaIndex: Int? = null,
        var boundSurfaceIndex: Int? = null,
        var isReady: Boolean = false,
        var hasRenderedFirstFrame: Boolean = false,
        var warmOnFirstFrame: Boolean = false,
    ) {
        val isHot: Boolean
            get() = isReady && hasRenderedFirstFrame
    }

    private class DefaultTargetPreloadStatusControl(
        private val policy: PreloadPolicy,
    ) : TargetPreloadStatusControl<Int, DefaultPreloadManager.PreloadStatus> {
        var currentPlayingIndex: Int = C.INDEX_UNSET
        var predictedIndex: Int = C.INDEX_UNSET

        @Suppress("CyclomaticComplexMethod", "ReturnCount", "MagicNumber")
        override fun getTargetPreloadStatus(rankingData: Int): DefaultPreloadManager.PreloadStatus {
            val center = if (predictedIndex != C.INDEX_UNSET) predictedIndex else currentPlayingIndex
            if (center == C.INDEX_UNSET) {
                return DefaultPreloadManager.PreloadStatus.PRELOAD_STATUS_NOT_PRELOADED
            }
            val distance = rankingData - center
            val absDistance = abs(distance)
            if (!policy.usePreparedNextPlayer) {
                return when {
                    distance == 0 -> {
                        DefaultPreloadManager.PreloadStatus.PRELOAD_STATUS_NOT_PRELOADED
                    }

                    distance > 0 && absDistance <= policy.diskPrefetchNext -> {
                        DefaultPreloadManager.PreloadStatus.specifiedRangeCached(5_000L)
                    }

                    else -> {
                        DefaultPreloadManager.PreloadStatus.PRELOAD_STATUS_NOT_PRELOADED
                    }
                }
            }
            return when {
                distance == 0 -> {
                    DefaultPreloadManager.PreloadStatus.PRELOAD_STATUS_NOT_PRELOADED
                }

                distance < 0 && absDistance <= policy.preparedPrev -> {
                    DefaultPreloadManager.PreloadStatus.specifiedRangeLoaded(1_000L)
                }

                distance > 0 && absDistance <= policy.preparedNext -> {
                    DefaultPreloadManager.PreloadStatus.specifiedRangeLoaded(1_000L)
                }

                distance > 0 && absDistance <= policy.diskPrefetchNext -> {
                    DefaultPreloadManager.PreloadStatus.specifiedRangeCached(5_000L)
                }

                else -> {
                    DefaultPreloadManager.PreloadStatus.PRELOAD_STATUS_NOT_PRELOADED
                }
            }
        }
    }
}
