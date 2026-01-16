package com.yral.shared.libs.videoplayback.android

import android.content.Context
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
import com.yral.shared.libs.videoplayback.PreloadPolicy
import com.yral.shared.libs.videoplayback.VideoSurfaceHandle
import com.yral.shared.libs.videoplayback.cacheKey
import com.yral.shared.libs.videoplayback.PreloadEventScheduler
import com.yral.shared.libs.videoplayback.PreparedSlotScheduler
import com.yral.shared.libs.videoplayback.PlaybackProgress
import com.yral.shared.libs.videoplayback.PlaybackProgressTicker
import com.yral.shared.libs.videoplayback.ui.AndroidVideoSurfaceHandle
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.math.abs
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(UnstableApi::class)
fun createAndroidPlaybackCoordinator(
    context: Context,
    deps: CoordinatorDeps = CoordinatorDeps(),
): PlaybackCoordinator = AndroidPlaybackCoordinator(context, deps)

@OptIn(UnstableApi::class)
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
    private val preloadManagerBuilder =
        DefaultPreloadManager.Builder(appContext, preloadStatusControl)
            .setCache(cache)
            .setLoadControl(createLoadControl())

    private val preloadManager = preloadManagerBuilder.build()

    private val playerA = createPlayer(preloadManagerBuilder)
    private val playerB = if (policy.usePreparedNextPlayer) createPlayer(preloadManagerBuilder) else null
    private var activeSlot = PlayerSlot(playerA)
    private var preparedSlot: PlayerSlot? = playerB?.let { PlayerSlot(it) }

    private val surfaces = mutableMapOf<Int, VideoSurfaceHandle>()
    private var feed: List<MediaDescriptor> = emptyList()
    private var mediaItems: List<MediaItem> = emptyList()
    private var mediaIndexById: Map<String, Int> = emptyMap()

    private var activeIndex: Int = -1
    private var predictedIndex: Int = -1
    private val preloadScheduler = PreloadEventScheduler(policy, reporter)
    private val preparedScheduler = PreparedSlotScheduler(policy, reporter)

    private val playStartMsById = mutableMapOf<String, Long>()
    private var firstFramePendingIndex: Int? = null
    private var rebuffering = false
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
            },
        )

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val index = activeSlot.index ?: return
            val item = feed.getOrNull(index) ?: return

            if (playbackState == Player.STATE_BUFFERING && activeSlot.player.playWhenReady) {
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

        override fun onPlayerError(error: PlaybackException) {
            val index = activeSlot.index ?: return
            val item = feed.getOrNull(index) ?: return
            reporter.playbackError(
                id = item.id,
                index = index,
                category = error.errorCodeName,
                code = error.errorCode,
                message = error.message,
            )
        }

        override fun onRenderedFirstFrame() {
            val index = activeSlot.index ?: return
            val item = feed.getOrNull(index) ?: return
            if (firstFramePendingIndex != index) return
            firstFramePendingIndex = null
            val start = playStartMsById[item.id] ?: return
            val elapsed = nowMs() - start
            reporter.firstFrameRendered(item.id, index)
            reporter.timeToFirstFrame(item.id, index, elapsed)
        }
    }

    private val preloadListener = object : PreloadManagerListener {
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
        playerA.addListener(listener)
        playerB?.addListener(listener)
        preloadManager.addListener(preloadListener)
        progressTicker.start()
    }

    override fun setFeed(items: List<MediaDescriptor>) {
        preloadScheduler.reset("feed_update") { feed.getOrNull(it)?.id }
        preparedScheduler.reset("feed_update") { feed.getOrNull(it)?.id }
        if (mediaItems.isNotEmpty()) {
            preloadManager.removeMediaItems(mediaItems)
        }
        feed = items
        mediaItems = items.mapIndexed { index, descriptor -> buildMediaItem(descriptor, index) }
        mediaIndexById = items.mapIndexed { index, descriptor -> descriptor.id to index }.toMap()

        if (mediaItems.isNotEmpty()) {
            val ranking = mediaItems.indices.toList()
            preloadManager.addMediaItems(mediaItems, ranking)
        }

        if (activeIndex >= items.size) {
            setActiveIndex(items.lastIndex)
        }
    }

    override fun appendFeed(items: List<MediaDescriptor>) {
        if (items.isEmpty()) return
        val startIndex = feed.size
        feed = feed + items
        val appendedMediaItems = items.mapIndexed { offset, descriptor ->
            buildMediaItem(descriptor, startIndex + offset)
        }
        mediaItems = mediaItems + appendedMediaItems
        mediaIndexById = feed.mapIndexed { index, descriptor -> descriptor.id to index }.toMap()
        val ranking = (startIndex until feed.size).toList()
        preloadManager.addMediaItems(appendedMediaItems, ranking)

        if (activeIndex == -1 && feed.isNotEmpty()) {
            setActiveIndex(0)
        }
    }

    override fun setActiveIndex(index: Int) {
        if (index !in feed.indices) return
        if (index == activeIndex && activeSlot.index == index) return

        activeIndex = index
        predictedIndex = index
        preloadStatusControl.currentPlayingIndex = index
        preloadStatusControl.predictedIndex = index
        preloadManager.setCurrentPlayingIndex(index)
        preloadManager.invalidate()

        preloadScheduler.update(index, feed.size) { feed.getOrNull(it)?.id }

        val item = feed[index]
        reporter.feedItemImpression(item.id, index)
        reporter.playStartRequest(item.id, index, "activeIndex")
        playStartMsById[item.id] = nowMs()
        firstFramePendingIndex = index

        if (preparedSlot?.index == index) {
            swapSlots()
        } else {
            prepareSlot(activeSlot, index, playWhenReady = true)
        }

        attachIfBound(activeSlot)
        activeSlot.player.playWhenReady = true

        schedulePreparedSlot(index)
    }

    override fun setScrollHint(predictedIndex: Int, velocity: Float?) {
        if (predictedIndex !in feed.indices) return
        if (predictedIndex == this.predictedIndex) return
        this.predictedIndex = predictedIndex
        preloadStatusControl.predictedIndex = predictedIndex
        preloadManager.invalidate()
        preloadScheduler.update(predictedIndex, feed.size) { feed.getOrNull(it)?.id }
    }

    override fun bindSurface(index: Int, surface: VideoSurfaceHandle) {
        surfaces[index] = surface
        if (activeSlot.index == index) {
            attachIfBound(activeSlot)
        } else if (preparedSlot?.index == index) {
            preparedSlot?.let { attachIfBound(it) }
        }
    }

    override fun unbindSurface(index: Int) {
        val handle = surfaces.remove(index)
        if (handle is AndroidVideoSurfaceHandle) {
            if (activeSlot.index == index && handle.playerState.value == activeSlot.player) {
                handle.playerState.value = null
            }
            val prepared = preparedSlot
            if (prepared != null &&
                prepared.index == index &&
                handle.playerState.value == prepared.player
            ) {
                handle.playerState.value = null
            }
        }
    }

    override fun onAppForeground() {
        if (activeSlot.index in feed.indices) {
            activeSlot.player.playWhenReady = true
        }
    }

    override fun onAppBackground() {
        activeSlot.player.playWhenReady = false
        preparedSlot?.player?.playWhenReady = false
    }

    override fun release() {
        preloadScheduler.reset("release") { feed.getOrNull(it)?.id }
        preparedScheduler.reset("release") { feed.getOrNull(it)?.id }
        progressTicker.stop()
        scope.cancel()
        playerA.release()
        playerB?.release()
        preloadManager.release()
        ShortformCacheProvider.release()
    }

    

    private fun schedulePreparedSlot(activeIndex: Int) {
        val prepared = preparedSlot ?: return
        preparedScheduler.schedule(activeIndex, feed.size, { feed.getOrNull(it)?.id }) { nextIndex ->
            if (prepared.index != nextIndex) {
                prepareSlot(prepared, nextIndex, playWhenReady = false)
            }
        }
    }

    private fun activeProgress(): PlaybackProgress? {
        val index = activeSlot.index ?: return null
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
        activeSlot = prepared
        preparedSlot = previousActive
        preparedScheduler.clearOnSwap()
    }

    private fun prepareSlot(slot: PlayerSlot, index: Int, playWhenReady: Boolean) {
        val mediaItem = mediaItems.getOrNull(index) ?: return
        val previousIndex = slot.index
        if (previousIndex != null && previousIndex != index) {
            detachSurface(previousIndex, slot.player)
        }
        var mediaSource = preloadManager.getMediaSource(mediaItem)
        if (mediaSource == null) {
            preloadManager.add(mediaItem, index)
            preloadManager.invalidate()
            mediaSource = preloadManager.getMediaSource(mediaItem)
        }
        if (mediaSource == null) return
        slot.index = index
        slot.player.setMediaSource(mediaSource)
        slot.player.prepare()
        slot.player.playWhenReady = playWhenReady
    }

    private fun attachIfBound(slot: PlayerSlot) {
        val index = slot.index ?: return
        val handle = surfaces[index]
        if (handle is AndroidVideoSurfaceHandle) {
            if (handle.playerState.value != slot.player) {
                handle.playerState.value = slot.player
            }
        }
    }

    private fun detachSurface(index: Int, player: Player) {
        val handle = surfaces[index] as? AndroidVideoSurfaceHandle ?: return
        if (handle.playerState.value == player) {
            handle.playerState.value = null
        }
    }

    private fun buildMediaItem(descriptor: MediaDescriptor, index: Int): MediaItem {
        val builder = MediaItem.Builder()
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
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()
        return preloadManagerBuilder.buildExoPlayer().apply {
            setAudioAttributes(audioAttributes, true)
            setHandleAudioBecomingNoisy(true)
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    private fun createLoadControl(): DefaultLoadControl {
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                5_000,
                20_000,
                500,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }

    private object ShortformCacheProvider {
        private val lock = Any()
        private var cache: SimpleCache? = null
        private var databaseProvider: StandaloneDatabaseProvider? = null
        private var refCount = 0

        fun acquire(context: Context, policy: PreloadPolicy): SimpleCache {
            synchronized(lock) {
                val existing = cache
                if (existing != null) {
                    refCount++
                    return existing
                }
                val cacheDir = File(context.cacheDir, "video-playback-cache")
                val evictor = LeastRecentlyUsedCacheEvictor(policy.cacheMaxBytes)
                val provider = databaseProvider ?: StandaloneDatabaseProvider(context).also {
                    databaseProvider = it
                }
                val created = SimpleCache(cacheDir, evictor, provider)
                cache = created
                refCount = 1
                return created
            }
        }

        fun release() {
            synchronized(lock) {
                if (refCount > 0) {
                    refCount--
                }
            }
        }
    }

    private data class PlayerSlot(
        val player: ExoPlayer,
        var index: Int? = null,
    )

    private class DefaultTargetPreloadStatusControl(
        private val policy: PreloadPolicy,
    ) : TargetPreloadStatusControl<Int, DefaultPreloadManager.PreloadStatus> {
        var currentPlayingIndex: Int = C.INDEX_UNSET
        var predictedIndex: Int = C.INDEX_UNSET

        override fun getTargetPreloadStatus(rankingData: Int): DefaultPreloadManager.PreloadStatus {
            val center = if (predictedIndex != C.INDEX_UNSET) predictedIndex else currentPlayingIndex
            if (center == C.INDEX_UNSET) {
                return DefaultPreloadManager.PreloadStatus.PRELOAD_STATUS_NOT_PRELOADED
            }
            val distance = rankingData - center
            val absDistance = abs(distance)
            if (!policy.usePreparedNextPlayer) {
                return when {
                    distance == 0 -> DefaultPreloadManager.PreloadStatus.PRELOAD_STATUS_NOT_PRELOADED
                    distance > 0 && absDistance <= policy.diskPrefetchNext ->
                        DefaultPreloadManager.PreloadStatus.specifiedRangeCached(5_000L)
                    else -> DefaultPreloadManager.PreloadStatus.PRELOAD_STATUS_NOT_PRELOADED
                }
            }
            return when {
                distance == 0 -> DefaultPreloadManager.PreloadStatus.PRELOAD_STATUS_NOT_PRELOADED
                distance < 0 && absDistance <= policy.preparedPrev ->
                    DefaultPreloadManager.PreloadStatus.specifiedRangeLoaded(1_000L)
                distance > 0 && absDistance <= policy.preparedNext ->
                    DefaultPreloadManager.PreloadStatus.specifiedRangeLoaded(1_000L)
                distance > 0 && absDistance <= policy.diskPrefetchNext ->
                    DefaultPreloadManager.PreloadStatus.specifiedRangeCached(5_000L)
                else -> DefaultPreloadManager.PreloadStatus.PRELOAD_STATUS_NOT_PRELOADED
            }
        }
    }
}
