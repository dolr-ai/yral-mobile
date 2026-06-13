package com.yral.shared.libs.videoplayback.ios

import com.yral.shared.libs.videoplayback.CoordinatorDeps
import com.yral.shared.libs.videoplayback.FirstFrameStartupAction
import com.yral.shared.libs.videoplayback.FirstFrameStartupWatchdog
import com.yral.shared.libs.videoplayback.MediaDescriptor
import com.yral.shared.libs.videoplayback.PlaybackCoordinator
import com.yral.shared.libs.videoplayback.PlaybackProgress
import com.yral.shared.libs.videoplayback.PlaybackProgressTicker
import com.yral.shared.libs.videoplayback.PreloadEventScheduler
import com.yral.shared.libs.videoplayback.PreparedSlotScheduler
import com.yral.shared.libs.videoplayback.VideoSurfaceHandle
import com.yral.shared.libs.videoplayback.planFeedAlignment
import com.yral.shared.libs.videoplayback.ui.IosVideoSurfaceHandle
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerItemFailedToPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerItemPlaybackStalledNotification
import platform.AVFoundation.AVPlayerItemStatusFailed
import platform.AVFoundation.AVPlayerItemStatusReadyToPlay
import platform.AVFoundation.AVPlayerTimeControlStatusPlaying
import platform.AVFoundation.AVPlayerTimeControlStatusWaitingToPlayAtSpecifiedRate
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.CMTimeRangeValue
import platform.AVFoundation.asset
import platform.AVFoundation.automaticallyWaitsToMinimizeStalling
import platform.AVFoundation.cancelLoading
import platform.AVFoundation.currentItem
import platform.AVFoundation.currentTime
import platform.AVFoundation.duration
import platform.AVFoundation.loadedTimeRanges
import platform.AVFoundation.pause
import platform.AVFoundation.playImmediatelyAtRate
import platform.AVFoundation.preferredForwardBufferDuration
import platform.AVFoundation.prerollAtRate
import platform.AVFoundation.rate
import platform.AVFoundation.reasonForWaitingToPlay
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.seekToTime
import platform.AVFoundation.timeControlStatus
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMake
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSRunLoop
import platform.Foundation.NSRunLoopCommonModes
import platform.Foundation.NSTimer
import platform.Foundation.NSURL
import platform.Foundation.NSValue
import platform.darwin.NSObjectProtocol
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_queue_create
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Suppress("MaxLineLength")
fun createIosPlaybackCoordinator(deps: CoordinatorDeps = CoordinatorDeps()): PlaybackCoordinator = IosPlaybackCoordinator(deps)

@Suppress("TooManyFunctions", "LargeClass")
private class IosPlaybackCoordinator(
    private val deps: CoordinatorDeps,
) : PlaybackCoordinator {
    private val reporter = deps.reporter
    private val policy = deps.policy

    @OptIn(ExperimentalTime::class)
    private val nowMs: () -> Long = { Clock.System.now().toEpochMilliseconds() }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var pollTimer: NSTimer? = null

    private val playerA = AVPlayer()
    private val playerB = if (policy.usePreparedNextPlayer) AVPlayer() else null
    private var activeSlot = PlayerSlot(playerA)
    private var preparedSlot: PlayerSlot? = playerB?.let { PlayerSlot(it) }

    private val surfaces = mutableMapOf<Int, VideoSurfaceHandle>()
    private var feed: List<MediaDescriptor> = emptyList()
    private var activeIndex: Int = -1
    private var predictedIndex: Int = -1
    private var userInteracting: Boolean = false
    private var pendingDiskPrefetchCenterIndex: Int? = null
    private var pendingActiveIndex: Int? = null
    private var appInBackground: Boolean = false
    private var released: Boolean = false

    private val playStartMsById = mutableMapOf<String, Long>()
    private var firstFramePendingIndex: Int? = null
    private val firstFrameStartupWatchdog = FirstFrameStartupWatchdog()
    private var startupStallStartMs: Long? = null

    private var rebuffering = false
    private var rebufferStartMs: Long? = null
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

    private val preloadScheduler = PreloadEventScheduler(policy, reporter)
    private val preparedScheduler = PreparedSlotScheduler(policy, reporter)

    private val cache = IosDownloadCache(policy.cacheMaxBytes)

    private val observers = mutableListOf<NSObjectProtocol>()

    init {
        IosAudioSession.ensurePlaybackSessionActive()
        playerA.automaticallyWaitsToMinimizeStalling = false
        playerB?.automaticallyWaitsToMinimizeStalling = false
        registerObservers()
        startPolling()
        progressTicker.start()
    }

    override fun setFeed(items: List<MediaDescriptor>) {
        val previousFeed = feed
        val previousIds = previousFeed.map { it.id }
        val currentIds = items.map { it.id }
        val alignment =
            planFeedAlignment(
                previousIds = previousIds,
                currentIds = currentIds,
                activeIndex = activeIndex,
                activeSlotIndex = activeSlot.index,
                preparedSlotIndex = preparedSlot?.index,
            )
        cancelPrefetch(reason = "feed_update")
        preparedScheduler.reset("feed_update") { feed.getOrNull(it)?.id }
        feed = items

        alignment.invalidatePreparedIndex?.let { stalePreparedIndex ->
            preparedSlot?.let { slot ->
                slot.player.pause()
                slot.itemGeneration++
                detachSurface(stalePreparedIndex, slot.player)
                slot.index = null
                slot.source = null
            }
        }

        if (alignment.clearPlaybackState) {
            activeIndex = -1
            predictedIndex = -1
            rebuffering = false
            rebufferStartMs = null
            firstFramePendingIndex = null
            firstFrameStartupWatchdog.clear()
            startupStallStartMs = null
            activeSlot.player.pause()
            preparedSlot?.player?.pause()
            activeSlot.index = null
            activeSlot.source = null
            activeSlot.itemGeneration++
            preparedSlot?.index = null
            preparedSlot?.source = null
            preparedSlot?.let { it.itemGeneration++ }
            pendingDiskPrefetchCenterIndex = null
            pendingActiveIndex = null
            return
        }

        // A pager-derived pending activation outlives a feed replacement: the pager will not
        // re-emit the page it already rests on, so it must win over the alignment's target.
        pendingActiveIndex = pendingActiveIndex?.takeIf { items.isNotEmpty() }?.coerceIn(0, items.lastIndex)
        alignment.nextActiveIndex?.let { targetIndex ->
            activeIndex = -1
            val target = pendingActiveIndex ?: targetIndex
            pendingActiveIndex = null
            setActiveIndex(target)
        }
    }

    override fun appendFeed(items: List<MediaDescriptor>) {
        if (items.isEmpty()) return
        feed = feed + items
        if (activeIndex == -1 && pendingActiveIndex == null && feed.isNotEmpty()) {
            setActiveIndex(0)
        }
    }

    @Suppress("ReturnCount")
    override fun setActiveIndex(index: Int) {
        if (index !in feed.indices) return
        if (userInteracting) {
            // currentPage flips at the 50% crossing, mid-drag/mid-fling. Replacing player
            // items here blocks the main thread inside the scroll animation, so full
            // activation defers to flushPendingActivation() on settle. Bare pause()/play()
            // calls are cheap though: the outgoing video is silenced at the crossing, and
            // when the target is the already-prepared next video it starts playing right
            // away (its item is loaded and prerolled; attach happened at the previous
            // settle via schedulePreparedSlot).
            val prepared = preparedSlot
            if (index != activeIndex) {
                activeSlot.player.pause()
                if (canEarlyPlayPrepared(prepared, index)) {
                    prepared?.player?.playImmediatelyAtRate(1.0F)
                } else {
                    prepared?.player?.pause()
                }
            } else {
                // Rocked back across the 50% line: swap audio back. An awaiting slot still
                // holds the outgoing item — its replace completion starts playback instead.
                prepared?.player?.pause()
                if (!appInBackground && !activeSlot.awaitingItem) {
                    activeSlot.player.playImmediatelyAtRate(1.0F)
                }
            }
            pendingActiveIndex = index
            return
        }
        if (index == activeIndex && activeSlot.index == index) {
            // Re-activating the page we never left (settle after a bounce-back): undo the
            // gesture-window pause. A no-op for already-playing re-emissions.
            if (!activeSlot.awaitingItem) {
                activeSlot.player.playImmediatelyAtRate(1.0F)
            }
            enforceSingleActivePlayback()
            return
        }

        endStartupStall(activeIndex)
        activeIndex = index
        predictedIndex = index
        rebuffering = false
        rebufferStartMs = null
        val item = feed[index]

        reporter.feedItemImpression(item.id, index)
        reporter.playStartRequest(item.id, index, "activeIndex")
        playStartMsById[item.id] = nowMs()
        firstFramePendingIndex = index
        firstFrameStartupWatchdog.start(index, nowMs())

        activateSlotsFor(index)

        schedulePreparedSlot(index)
        scheduleDiskPrefetch(index)
    }

    private fun activateSlotsFor(index: Int) {
        val prepared = preparedSlot
        if (prepared != null && prepared.index == index && !prepared.awaitingItem) {
            swapSlots()
            attachIfBound(activeSlot)
            activeSlot.player.playImmediatelyAtRate(1.0F)
        } else {
            if (prepared != null && prepared.index == index) {
                // The prepared slot's item swap is still in flight; supersede it rather than
                // promoting a player that may still hold the previous video.
                prepared.itemGeneration++
                prepared.index = null
                prepared.source = null
                preparedScheduler.reset("superseded") { feed.getOrNull(it)?.id }
            }
            prepareSlot(activeSlot, index, shouldPlay = true)
            attachIfBound(activeSlot)
        }
        enforceSingleActivePlayback()
    }

    override fun setScrollHint(
        predictedIndex: Int,
        velocity: Float?,
    ) {
        if (predictedIndex !in feed.indices) return
        if (predictedIndex == this.predictedIndex) return
        this.predictedIndex = predictedIndex
        scheduleDiskPrefetch(predictedIndex)
    }

    override fun setUserInteracting(isInteracting: Boolean) {
        if (userInteracting == isInteracting) return
        userInteracting = isInteracting
        // In-flight cache downloads survive touch-down on purpose: cancelling them per gesture
        // meant the cache never filled during continuous scrolling. New starts stay deferred
        // while interacting (scheduleDiskPrefetch).
        if (!isInteracting) {
            flushPendingActivation()
            pendingDiskPrefetchCenterIndex?.let { centerIndex ->
                pendingDiskPrefetchCenterIndex = null
                scheduleDiskPrefetch(centerIndex)
            }
        }
    }

    // Early play must be a bare playImmediatelyAtRate: the item swap landed and the player
    // was attached to the target page's surface at a prior settle. If either is still
    // missing, stay silent and let the settle flush do the full activation — attaching or
    // building anything inside the gesture window is what froze the fling.
    @Suppress("ReturnCount")
    private fun canEarlyPlayPrepared(
        prepared: PlayerSlot?,
        index: Int,
    ): Boolean {
        if (prepared == null || appInBackground) return false
        if (prepared.index != index || prepared.awaitingItem) return false
        val handle = surfaces[index] as? IosVideoSurfaceHandle ?: return false
        return handle.controller.player == prepared.player
    }

    private fun flushPendingActivation() {
        if (appInBackground) return
        val pending = pendingActiveIndex ?: return
        pendingActiveIndex = null
        // The activation schedules prefetch around the settled index; drop any stale predicted
        // center from mid-gesture scroll hints so it can't shift the window afterwards.
        pendingDiskPrefetchCenterIndex = null
        setActiveIndex(pending)
    }

    override fun bindSurface(
        index: Int,
        surface: VideoSurfaceHandle,
    ) {
        surfaces[index] = surface
        if (activeSlot.index == index) {
            attachIfBound(activeSlot)
        } else if (preparedSlot?.index == index) {
            preparedSlot?.let { attachIfBound(it) }
        }
        enforceSingleActivePlayback()
    }

    override fun unbindSurface(
        index: Int,
        surfaceId: String,
    ) {
        val handle = surfaces[index]
        if (handle?.id != surfaceId) return
        surfaces.remove(index)
        if (handle is IosVideoSurfaceHandle) {
            if (activeSlot.index == index && handle.controller.player == activeSlot.player) {
                handle.controller.player = null
                handle.playerState.value = null
            }
            val prepared = preparedSlot
            if (prepared != null &&
                prepared.index == index &&
                handle.controller.player == prepared.player
            ) {
                handle.controller.player = null
                handle.playerState.value = null
            }
        }
    }

    override fun onAppForeground() {
        appInBackground = false
        if (!userInteracting) {
            val pending = pendingActiveIndex
            if (pending != null && pending != activeIndex) {
                flushPendingActivation()
                return
            }
            pendingActiveIndex = null
        }
        // While an item swap is in flight the player still holds the outgoing item; the
        // replace completion starts playback instead (appInBackground is false again now).
        if (activeSlot.index in feed.indices && !activeSlot.awaitingItem) {
            activeSlot.player.playImmediatelyAtRate(1.0F)
            enforceSingleActivePlayback()
        }
    }

    override fun onAppBackground() {
        // Keep pendingActiveIndex: a late isScrollInProgress=false emission must not start
        // playback while backgrounded; onAppForeground flushes it instead.
        appInBackground = true
        activeSlot.player.pause()
        preparedSlot?.player?.pause()
        cancelPrefetch(reason = "background")
        pendingDiskPrefetchCenterIndex = null
    }

    override fun release() {
        released = true
        pollTimer?.invalidate()
        pollTimer = null
        observers.forEach { NSNotificationCenter.defaultCenter.removeObserver(it) }
        observers.clear()
        activeSlot.player.pause()
        preparedSlot?.player?.pause()
        cancelPrefetch(reason = "release")
        pendingDiskPrefetchCenterIndex = null
        pendingActiveIndex = null
        cache.close()
        preparedScheduler.reset("release") { feed.getOrNull(it)?.id }
        firstFrameStartupWatchdog.clear()
        startupStallStartMs = null
        progressTicker.stop()
        scope.cancel()
    }

    private fun schedulePreparedSlot(activeIndex: Int) {
        val prepared = preparedSlot ?: return
        preparedScheduler.schedule(activeIndex, feed.size, { feed.getOrNull(it)?.id }) { nextIndex ->
            if (prepared.index != nextIndex) {
                // The attach happens in the replace completion (settle time) so an early play
                // at the next 50% crossing shows video immediately — bindSurface ran before
                // this slot was prepared, so nothing else attaches it until the swap.
                prepareSlot(prepared, nextIndex, shouldPlay = false) {
                    preparedScheduler.setStartTime(nowMs())
                }
            } else {
                prepared.player.pause()
                attachIfBound(prepared)
                preparedScheduler.setStartTime(nowMs())
                enforceSingleActivePlayback()
            }
        }
    }

    private fun swapSlots() {
        val prepared = preparedSlot ?: return
        val previousActive = activeSlot
        val previousActiveIndex = previousActive.index
        previousActive.player.pause()
        if (previousActiveIndex != null) {
            detachSurface(previousActiveIndex, previousActive.player)
        }
        activeSlot = prepared
        preparedSlot = previousActive
        previousActive.index = null
        previousActive.source = null
        preparedScheduler.clearOnSwap()
    }

    @Suppress("LoopWithTooManyJumpStatements")
    private fun scheduleDiskPrefetch(centerIndex: Int) {
        if (userInteracting) {
            pendingDiskPrefetchCenterIndex = centerIndex
            return
        }
        val result = preloadScheduler.update(centerIndex, feed.size) { feed.getOrNull(it)?.id }
        for (index in result.toCancel) {
            feed.getOrNull(index)?.let { item ->
                cache.cancelPrefetch(item)
            }
        }

        var startedPrefetchCount = 0
        for (index in result.toStart) {
            if (index !in result.window.disk) continue
            if (startedPrefetchCount >= MAX_SCROLL_SETTLED_PREFETCH_STARTS) continue
            val item = feed.getOrNull(index) ?: continue
            startedPrefetchCount++
            cache.prefetch(
                descriptor = item,
                onComplete = { bytes, fromCache ->
                    reporter.preloadCompleted(item.id, index, bytes, 0, fromCache)
                },
                onError = {
                    reporter.preloadCanceled(item.id, index, "error")
                },
            )
        }
    }

    private fun cancelPrefetch(reason: String) {
        preloadScheduler.reset(reason) { feed.getOrNull(it)?.id }
        cache.cancelAll()
        pendingDiskPrefetchCenterIndex = null
    }

    private fun prepareSlot(
        slot: PlayerSlot,
        index: Int,
        shouldPlay: Boolean,
        onReplaced: (() -> Unit)? = null,
    ) {
        val descriptor = feed[index]
        val itemBuild = buildPlayerItem(descriptor, index)
        val item = itemBuild.item
        item.preferredForwardBufferDuration = 1.0
        val previousIndex = slot.index
        if (previousIndex != null && previousIndex != index) {
            detachSurface(previousIndex, slot.player)
        }
        slot.index = index
        slot.source = itemBuild.source
        // The player keeps its outgoing item until the queued swap lands; stay silent for
        // that whole window — the completion decides whether the new item starts.
        slot.player.pause()
        replaceItemAsync(slot, item) {
            if (slot.index != index) return@replaceItemAsync
            if (shouldPlay) {
                val stillActiveTarget = slot === activeSlot && activeIndex == index
                val departing = pendingActiveIndex != null && pendingActiveIndex != index
                if (stillActiveTarget && !appInBackground && !departing) {
                    slot.player.playImmediatelyAtRate(1.0F)
                }
            }
            if (!userInteracting) {
                attachIfBound(slot)
            }
            enforceSingleActivePlayback()
            onReplaced?.invoke()
        }
    }

    // Hands the expensive item swap to the slot's serial queue. Only the completion of the
    // newest generation touches coordinator state, back on the main thread.
    private fun replaceItemAsync(
        slot: PlayerSlot,
        item: AVPlayerItem,
        onReplaced: () -> Unit,
    ) {
        val generation = ++slot.itemGeneration
        val player = slot.player
        val outgoingAsset = player.currentItem?.asset as? AVURLAsset
        dispatch_async(slot.itemOpsQueue) {
            outgoingAsset?.cancelLoading()
            player.replaceCurrentItemWithPlayerItem(item)
            dispatch_async(dispatch_get_main_queue()) {
                if (released || generation != slot.itemGeneration) return@dispatch_async
                slot.completedItemGeneration = generation
                onReplaced()
            }
        }
    }

    private fun attachIfBound(slot: PlayerSlot) {
        val index = slot.index ?: return
        val handle = surfaces[index]
        if (handle is IosVideoSurfaceHandle) {
            if (handle.controller.player != slot.player) {
                handle.controller.player = slot.player
                handle.playerState.value = slot.player
            }
        }
    }

    private fun detachSurface(
        index: Int,
        player: AVPlayer,
    ) {
        val handle = surfaces[index] as? IosVideoSurfaceHandle ?: return
        if (handle.controller.player == player) {
            handle.controller.player = null
            handle.playerState.value = null
        }
    }

    private fun enforceSingleActivePlayback() {
        if (activeSlot.index != activeIndex) {
            activeSlot.player.pause()
        }
        val prepared = preparedSlot
        if (prepared != null) {
            val preparedIndex = prepared.index
            // Mid-gesture this runs from bindSurface as new pages compose; it must not pause
            // a prepared player that setActiveIndex early-started at the 50% crossing.
            val earlyPlaying =
                userInteracting && preparedIndex != null && preparedIndex == pendingActiveIndex
            if (!earlyPlaying) {
                prepared.player.pause()
            }
            if (preparedIndex != null && preparedIndex == activeIndex) {
                detachSurface(preparedIndex, prepared.player)
            }
        }
        detachStaleSurfacePlayers()
    }

    private fun detachStaleSurfacePlayers() {
        surfaces.forEach { (index, surface) ->
            val handle = surface as? IosVideoSurfaceHandle ?: return@forEach
            val player = handle.controller.player ?: return@forEach
            val isValidActiveSurface = index == activeIndex && activeSlot.index == index && player == activeSlot.player
            val isValidPreparedSurface = preparedSlot?.let { it.index == index && player == it.player } == true
            if (!isValidActiveSurface && !isValidPreparedSurface) {
                handle.controller.player = null
                handle.playerState.value = null
            }
        }
    }

    private fun buildPlayerItem(
        descriptor: MediaDescriptor,
        index: Int,
    ): PlayerItemBuildResult {
        val cachedUrl = cache.cachedFileUrl(descriptor)
        val url =
            cachedUrl ?: NSURL.URLWithString(descriptor.uri) ?: run {
                val message = "Invalid media URL for id=${descriptor.id}, uri=${descriptor.uri}"
                reporter.playbackError(descriptor.id, index, "invalid_url", "ios", message)
                return PlayerItemBuildResult(
                    item = AVPlayerItem(),
                    source =
                        PlaybackSourceDiagnostics(
                            mediaId = descriptor.id,
                            index = index,
                            uri = descriptor.uri,
                            source = "invalid",
                            urlScheme = "invalid",
                            headersPresent = descriptor.headers.isNotEmpty(),
                            headerNames = descriptor.headers.keys.sorted(),
                            builtAtMs = nowMs(),
                        ),
                )
            }
        val source =
            PlaybackSourceDiagnostics(
                mediaId = descriptor.id,
                index = index,
                uri = descriptor.uri,
                source = if (cachedUrl != null) "cache" else "remote",
                urlScheme = url.scheme ?: "unknown",
                headersPresent = cachedUrl == null && descriptor.headers.isNotEmpty(),
                headerNames = if (cachedUrl == null) descriptor.headers.keys.sorted() else emptyList(),
                builtAtMs = nowMs(),
            )
        val options: Map<Any?, *>? =
            if (cachedUrl == null && descriptor.headers.isNotEmpty()) {
                mapOf("AVURLAssetHTTPHeaderFieldsKey" to descriptor.headers)
            } else {
                null
            }
        val asset = AVURLAsset(uRL = url, options = options)
        if (cachedUrl != null) {
            reporter.cacheHit(descriptor.id, 0)
        } else {
            reporter.cacheMiss(descriptor.id, 0)
        }
        return PlayerItemBuildResult(
            item = AVPlayerItem(asset = asset),
            source = source,
        )
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun registerObservers() {
        val center = NSNotificationCenter.defaultCenter

        observers +=
            center.addObserverForName(
                name = AVPlayerItemFailedToPlayToEndTimeNotification,
                `object` = null,
                queue = null,
            ) { notification ->
                val current = activeSlot.player.currentItem
                if (notification?.`object` == current) {
                    val index = activeSlot.index ?: return@addObserverForName
                    val item = feed.getOrNull(index) ?: return@addObserverForName
                    reporter.playbackError(item.id, index, "failed", "ios")
                }
            }

        observers +=
            center.addObserverForName(
                name = AVPlayerItemPlaybackStalledNotification,
                `object` = null,
                queue = null,
            ) { notification ->
                val current = activeSlot.player.currentItem
                if (notification?.`object` == current) {
                    val index = activeSlot.index ?: return@addObserverForName
                    val item = feed.getOrNull(index) ?: return@addObserverForName
                    reporter.rebufferStart(item.id, index, "stalled")
                    rebuffering = true
                    rebufferStartMs = nowMs()
                }
            }

        observers +=
            center.addObserverForName(
                name = AVPlayerItemDidPlayToEndTimeNotification,
                `object` = null,
                queue = null,
            ) { notification ->
                if (resumeEarlyPlayingPreparedAtEnd(notification?.`object`)) {
                    return@addObserverForName
                }
                val current = activeSlot.player.currentItem
                if (notification?.`object` == current) {
                    val index = activeSlot.index ?: return@addObserverForName
                    val item = feed.getOrNull(index) ?: return@addObserverForName
                    reporter.playbackEnded(item.id, index)
                    // Loop by seeking back: rebuilding the item re-streams the asset and
                    // replaceCurrentItemWithPlayerItem can block the main thread tearing down
                    // the old one. Same item => the notification filter above keeps matching.
                    activeSlot.player.seekToTime(CMTimeMake(value = 0, timescale = 1))
                    // An end notification can land just after the gesture-window pause that
                    // silences a video the user is scrolling away from — don't resurrect it.
                    val departing =
                        userInteracting && pendingActiveIndex != null && pendingActiveIndex != index
                    if (!departing) {
                        activeSlot.player.playImmediatelyAtRate(1.0F)
                    }
                }
            }
    }

    // An early-played prepared video (started at the 50% crossing) that reaches its end
    // before settle would otherwise park at its last frame with no recovery — the
    // end-notification's active-player match ignores it.
    @Suppress("ReturnCount")
    @OptIn(ExperimentalForeignApi::class)
    private fun resumeEarlyPlayingPreparedAtEnd(endedObject: Any?): Boolean {
        if (!userInteracting) return false
        val prepared = preparedSlot ?: return false
        if (prepared.awaitingItem) return false
        val earlyPlaying = prepared.index != null && prepared.index == pendingActiveIndex
        if (!earlyPlaying || endedObject != prepared.player.currentItem) return false
        prepared.player.seekToTime(CMTimeMake(value = 0, timescale = 1))
        prepared.player.playImmediatelyAtRate(1.0F)
        return true
    }

    private fun startPolling() {
        pollTimer?.invalidate()
        pollTimer =
            NSTimer.scheduledTimerWithTimeInterval(
                interval = 0.2,
                repeats = true,
            ) {
                scope.launch {
                    pollPlaybackState()
                }
            }
        NSRunLoop.mainRunLoop.addTimer(pollTimer!!, NSRunLoopCommonModes)
    }

    @Suppress("CyclomaticComplexMethod", "MagicNumber", "ReturnCount")
    @OptIn(ExperimentalForeignApi::class)
    private fun pollPlaybackState() {
        if (userInteracting) return
        val index = activeSlot.index ?: return
        val item = feed.getOrNull(index) ?: return

        // While an item swap is queued the player still reports on the OUTGOING item —
        // its advancing currentTime would fake a first frame for the new index and the
        // resume nudge would play the wrong video. Skip health checks until the swap lands.
        if (!activeSlot.awaitingItem) {
            pollActiveSlotState(index, item)
        }

        val prepared = preparedSlot
        if (prepared != null && !prepared.awaitingItem) {
            val preparedItem = prepared.player.currentItem
            val preparedIndex = prepared.index
            if (preparedIndex != null && preparedItem?.status == AVPlayerItemStatusReadyToPlay) {
                preparedScheduler.markReady(
                    index = preparedIndex,
                    nowMs = nowMs(),
                    idAt = { feed.getOrNull(it)?.id },
                ) {
                    prepared.player.prerollAtRate(1.0F) { _ ->
                        // A late completion must not pause a player that has since started
                        // for real: early play at the crossing (pendingActiveIndex) or a
                        // slot swap that promoted it to active.
                        if (prepared.index == preparedIndex &&
                            preparedIndex != pendingActiveIndex &&
                            preparedIndex != activeIndex
                        ) {
                            prepared.player.pause()
                        }
                    }
                }
            }
            if (preparedIndex != null && preparedItem?.status == AVPlayerItemStatusFailed) {
                preparedScheduler.markError(preparedIndex, { feed.getOrNull(it)?.id }, "error")
            }
        }
    }

    @Suppress("MagicNumber")
    @OptIn(ExperimentalForeignApi::class)
    private fun pollActiveSlotState(
        index: Int,
        item: MediaDescriptor,
    ) {
        val status = activeSlot.player.timeControlStatus
        if (status == AVPlayerTimeControlStatusWaitingToPlayAtSpecifiedRate && !rebuffering) {
            rebuffering = true
            rebufferStartMs = nowMs()
            reporter.rebufferStart(item.id, index, "buffering")
        }

        if (status == AVPlayerTimeControlStatusPlaying && rebuffering) {
            rebuffering = false
            val start = rebufferStartMs
            if (start != null) {
                reporter.rebufferEnd(item.id, index, "buffering")
                reporter.rebufferTotal(item.id, index, nowMs() - start)
            }
            rebufferStartMs = null
        }

        if (firstFramePendingIndex == index) {
            val seconds = CMTimeGetSeconds(activeSlot.player.currentTime())
            if (seconds > 0.01) {
                firstFramePendingIndex = null
                firstFrameStartupWatchdog.clear(index)
                endStartupStall(index)
                val start = playStartMsById[item.id] ?: nowMs()
                reporter.firstFrameRendered(item.id, index)
                reporter.timeToFirstFrame(item.id, index, nowMs() - start)
            }
        }

        recoverFirstFrameStartupIfNeeded(index, item)
    }

    private fun recoverFirstFrameStartupIfNeeded(
        index: Int,
        item: MediaDescriptor,
    ) {
        if (!canRecoverStartup(index)) return
        val action =
            firstFrameStartupWatchdog.evaluate(
                index = index,
                nowMs = nowMs(),
                firstFramePending = firstFramePendingIndex == index,
                hasBufferedMedia = activeItemHasBufferedMedia(),
            )
        when (action) {
            FirstFrameStartupAction.None -> {
                Unit
            }

            FirstFrameStartupAction.Resume -> {
                startStartupStall(index, item, "first_frame_resume")
                activeSlot.player.playImmediatelyAtRate(1.0F)
                enforceSingleActivePlayback()
            }

            FirstFrameStartupAction.Rebuild -> {
                startStartupStall(index, item, "first_frame_rebuild")
                val slot = activeSlot
                val replacementBuild = buildPlayerItem(item, index)
                val replacement = replacementBuild.item
                replacement.preferredForwardBufferDuration = 1.0
                slot.source = replacementBuild.source
                slot.player.pause()
                replaceItemAsync(slot, replacement) {
                    if (slot !== activeSlot || slot.index != index || activeIndex != index) {
                        return@replaceItemAsync
                    }
                    attachIfBound(slot)
                    val departing = pendingActiveIndex != null && pendingActiveIndex != index
                    if (!appInBackground && !departing) {
                        slot.player.playImmediatelyAtRate(1.0F)
                    }
                    enforceSingleActivePlayback()
                    firstFramePendingIndex = index
                    playStartMsById[item.id] = nowMs()
                }
            }

            FirstFrameStartupAction.GiveUp -> {
                firstFramePendingIndex = null
                firstFrameStartupWatchdog.clear(index)
                endStartupStall(index)
                reporter.playbackError(
                    id = item.id,
                    index = index,
                    category = "startup_timeout",
                    code = IOS_PLAYBACK_CODE,
                    message = startupDiagnosticMessage(index, item, "startup_timeout"),
                )
            }
        }
    }

    // A destructive rebuild restarts the stream from byte zero — on a slow network that
    // makes a progressing load strictly worse. Any buffered media means data is arriving.
    @OptIn(ExperimentalForeignApi::class)
    private fun activeItemHasBufferedMedia(): Boolean {
        val playerItem = activeSlot.player.currentItem ?: return false
        return playerItem.loadedTimeRanges.any { rangeValue ->
            val range = (rangeValue as? NSValue)?.CMTimeRangeValue ?: return@any false
            range.useContents { CMTimeGetSeconds(duration.readValue()) > MIN_BUFFERED_MEDIA_SECONDS }
        }
    }

    private fun canRecoverStartup(index: Int): Boolean =
        !userInteracting &&
            activeIndex == index &&
            activeSlot.index == index &&
            firstFramePendingIndex == index &&
            surfaceHasActivePlayer(index)

    private fun surfaceHasActivePlayer(index: Int): Boolean {
        val surface = surfaces[index] as? IosVideoSurfaceHandle ?: return false
        return surface.controller.player == activeSlot.player
    }

    @Suppress("CyclomaticComplexMethod")
    @OptIn(ExperimentalForeignApi::class)
    private fun startupDiagnosticMessage(
        index: Int,
        item: MediaDescriptor,
        action: String,
    ): String {
        val player = activeSlot.player
        val playerItem = player.currentItem
        val source = activeSlot.source
        val currentSeconds = CMTimeGetSeconds(player.currentTime()).finiteOrNull()
        val durationSeconds =
            playerItem
                ?.duration
                ?.let { CMTimeGetSeconds(it) }
                ?.finiteOrNull()
        val itemError = playerItem?.error
        val surface = surfaces[index] as? IosVideoSurfaceHandle
        val surfaceBound = surface != null
        val surfaceHasActivePlayer = surface?.controller?.player == player
        val playStartMs = playStartMsById[item.id]
        val elapsedMs = playStartMs?.let { nowMs() - it }
        val sourceAgeMs = source?.builtAtMs?.let { nowMs() - it }

        return buildList {
            add("action=$action")
            add("platform=ios")
            add("media_id=${item.id}")
            add("index=$index")
            add("active_index=$activeIndex")
            add("slot_index=${activeSlot.index}")
            elapsedMs?.let { add("elapsed_ms=$it") }
            add("first_frame_pending=${firstFramePendingIndex == index}")
            add("player_time_control_status=${player.timeControlStatus}")
            add("player_rate=${player.rate}")
            currentSeconds?.let { add("player_current_seconds=$it") }
            add("player_reason_for_waiting=${player.reasonForWaitingToPlay ?: "none"}")
            add("item_status=${playerItem?.status ?: "none"}")
            durationSeconds?.let { add("item_duration_seconds=$it") }
            itemError?.code?.let { add("item_error_code=$it") }
            itemError?.localizedDescription?.let { add("item_error_message=${it.compactForDiagnostics()}") }
            add("surface_bound=$surfaceBound")
            add("surface_has_active_player=$surfaceHasActivePlayer")
            add("source=${source?.source ?: "unknown"}")
            add("source_url_scheme=${source?.urlScheme ?: "unknown"}")
            add("source_headers_present=${source?.headersPresent ?: false}")
            source?.headerNames?.takeIf { it.isNotEmpty() }?.let {
                add("source_header_names=${it.joinToString(",").compactForDiagnostics()}")
            }
            sourceAgeMs?.let { add("source_age_ms=$it") }
            add("uri=${item.uri.compactForDiagnostics()}")
        }.joinToString(separator = " ")
    }

    private fun startStartupStall(
        index: Int,
        item: MediaDescriptor,
        reason: String,
    ) {
        if (startupStallStartMs != null) return
        startupStallStartMs = nowMs()
        reporter.stallStart(item.id, index, reason)
    }

    private fun endStartupStall(index: Int) {
        val start = startupStallStartMs ?: return
        val item =
            feed.getOrNull(index) ?: run {
                startupStallStartMs = null
                return
            }
        startupStallStartMs = null
        reporter.stallEnd(item.id, index, nowMs() - start)
    }

    private class PlayerSlot(
        val player: AVPlayer,
        var index: Int? = null,
        var source: PlaybackSourceDiagnostics? = null,
    ) {
        // replaceCurrentItemWithPlayerItem does a mostly-synchronous cross-queue wait inside
        // AVFCore; tearing down a mid-stream item there blocks the calling thread for
        // 100ms–seconds. It must never run on main (it froze scrolling), so each player owns
        // a serial queue for item swaps. Generations let a newer prepare drop the completion
        // of one it superseded.
        val itemOpsQueue = dispatch_queue_create("com.yral.videoplayback.item-ops", null)
        var itemGeneration: Long = 0L
        var completedItemGeneration: Long = 0L
        val awaitingItem: Boolean
            get() = itemGeneration != completedItemGeneration
    }

    private data class PlayerItemBuildResult(
        val item: AVPlayerItem,
        val source: PlaybackSourceDiagnostics,
    )

    private data class PlaybackSourceDiagnostics(
        val mediaId: String,
        val index: Int,
        val uri: String,
        val source: String,
        val urlScheme: String,
        val headersPresent: Boolean,
        val headerNames: List<String>,
        val builtAtMs: Long,
    )

    @Suppress("ReturnCount", "MagicNumber")
    @OptIn(ExperimentalForeignApi::class)
    private fun activeProgress(): PlaybackProgress? {
        val index = activeSlot.index ?: return null
        val item = feed.getOrNull(index) ?: return null
        val status = activeSlot.player.timeControlStatus
        if (status != AVPlayerTimeControlStatusPlaying) return null
        val currentSeconds = CMTimeGetSeconds(activeSlot.player.currentTime())
        val durationSeconds =
            activeSlot.player.currentItem
                ?.duration
                ?.let { CMTimeGetSeconds(it) }
        if (!currentSeconds.isFinite() || durationSeconds == null || !durationSeconds.isFinite()) {
            return null
        }
        val durationMs = (durationSeconds * 1000).toLong()
        if (durationMs <= 0L) return null
        val positionMs = (currentSeconds * 1000).toLong().coerceAtLeast(0L)
        return PlaybackProgress(
            id = item.id,
            index = index,
            positionMs = positionMs,
            durationMs = durationMs,
        )
    }

    private fun Double.finiteOrNull(): Double? =
        if (isFinite()) {
            this
        } else {
            null
        }

    private fun String.compactForDiagnostics(): String = replace(Regex("\\s+"), "_")

    private companion object {
        private const val IOS_PLAYBACK_CODE = "ios"
        private const val MAX_SCROLL_SETTLED_PREFETCH_STARTS = 3
        private const val MIN_BUFFERED_MEDIA_SECONDS = 0.25
    }
}
