package com.shortform.video.ios

import com.shortform.video.CoordinatorDeps
import com.shortform.video.MediaDescriptor
import com.shortform.video.PlaybackCoordinator
import com.shortform.video.VideoSurfaceHandle
import com.shortform.video.computePreloadWindow
import com.shortform.video.ui.IosVideoSurfaceHandle
import kotlinx.cinterop.ExperimentalForeignApi
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
import platform.AVFoundation.automaticallyWaitsToMinimizeStalling
import platform.AVFoundation.currentItem
import platform.AVFoundation.currentTime
import platform.AVFoundation.pause
import platform.AVFoundation.playImmediatelyAtRate
import platform.AVFoundation.preferredForwardBufferDuration
import platform.AVFoundation.prerollAtRate
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.timeControlStatus
import platform.CoreMedia.CMTimeGetSeconds
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSRunLoop
import platform.Foundation.NSRunLoopCommonModes
import platform.Foundation.NSTimer
import platform.Foundation.NSURL
import platform.darwin.NSObjectProtocol

fun createIosPlaybackCoordinator(
    deps: CoordinatorDeps = CoordinatorDeps(),
): PlaybackCoordinator = IosPlaybackCoordinator(deps)

private class IosPlaybackCoordinator(
    private val deps: CoordinatorDeps,
) : PlaybackCoordinator {
    private val reporter = deps.reporter
    private val policy = deps.policy
    private val nowMs = deps.nowMs

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

    private val playStartMsById = mutableMapOf<String, Long>()
    private var firstFramePendingIndex: Int? = null

    private var rebuffering = false
    private var rebufferStartMs: Long? = null

    private var preparedPendingIndex: Int? = null
    private var preparedStartMs: Long? = null
    private var preparedPrerollRequested: Boolean = false
    private var scheduledPrefetchTargets: Set<Int> = emptySet()

    private val cache = IosDownloadCache(policy.cacheMaxBytes)

    private val observers = mutableListOf<NSObjectProtocol>()

    init {
        playerA.automaticallyWaitsToMinimizeStalling = false
        playerB?.automaticallyWaitsToMinimizeStalling = false
        registerObservers()
        startPolling()
    }

    override fun setFeed(items: List<MediaDescriptor>) {
        cancelPrefetch(reason = "feed_update")
        feed = items
        if (activeIndex >= items.size) {
            setActiveIndex(items.lastIndex)
        }
    }

    override fun setActiveIndex(index: Int) {
        if (index !in feed.indices) return
        if (index == activeIndex && activeSlot.index == index) return

        activeIndex = index
        predictedIndex = index
        val item = feed[index]

        reporter.feedItemImpression(item.id, index)
        reporter.playStartRequest(item.id, index, "activeIndex")
        playStartMsById[item.id] = nowMs()
        firstFramePendingIndex = index

        if (preparedSlot?.index == index) {
            swapSlots()
            preparedPendingIndex = null
            preparedStartMs = null
            preparedPrerollRequested = false
        } else {
            prepareSlot(activeSlot, index, shouldPlay = true)
        }

        attachIfBound(activeSlot)
        activeSlot.player.playImmediatelyAtRate(1.0F)

        schedulePreparedSlot(index)
        scheduleDiskPrefetch(index)
    }

    override fun setScrollHint(predictedIndex: Int, velocity: Float?) {
        if (predictedIndex !in feed.indices) return
        if (predictedIndex == this.predictedIndex) return
        this.predictedIndex = predictedIndex
        scheduleDiskPrefetch(predictedIndex)
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
        if (handle is IosVideoSurfaceHandle) {
            if (activeSlot.index == index && handle.controller.player == activeSlot.player) {
                handle.controller.player = null
            }
            val prepared = preparedSlot
            if (prepared != null &&
                prepared.index == index &&
                handle.controller.player == prepared.player
            ) {
                handle.controller.player = null
            }
        }
    }

    override fun onAppForeground() {
        if (activeSlot.index in feed.indices) {
            activeSlot.player.playImmediatelyAtRate(1.0F)
        }
    }

    override fun onAppBackground() {
        activeSlot.player.pause()
        preparedSlot?.player?.pause()
        cancelPrefetch(reason = "background")
    }

    override fun release() {
        pollTimer?.invalidate()
        pollTimer = null
        observers.forEach { NSNotificationCenter.defaultCenter.removeObserver(it) }
        observers.clear()
        activeSlot.player.pause()
        preparedSlot?.player?.pause()
        cancelPrefetch(reason = "release")
        scope.cancel()
    }

    private fun schedulePreparedSlot(activeIndex: Int) {
        val prepared = preparedSlot ?: return
        val nextIndex = activeIndex + 1
        if (nextIndex in feed.indices) {
            if (prepared.index != nextIndex) {
                prepareSlot(prepared, nextIndex, shouldPlay = false)
                preparedPendingIndex = nextIndex
                preparedStartMs = nowMs()
                preparedPrerollRequested = false
                reporter.preloadScheduled(feed[nextIndex].id, nextIndex, 1, "prepared")
            }
        }
    }

    private fun swapSlots() {
        val prepared = preparedSlot ?: return
        val previousActive = activeSlot
        activeSlot = prepared
        preparedSlot = previousActive
    }

    private fun scheduleDiskPrefetch(centerIndex: Int) {
        val desired = computePreloadWindow(centerIndex, feed.size, policy).disk
        val toCancel = scheduledPrefetchTargets - desired
        val toStart = desired - scheduledPrefetchTargets

        for (index in toCancel) {
            feed.getOrNull(index)?.let { item ->
                cache.cancelPrefetch(item)
                reporter.preloadCanceled(item.id, index, "window_shift")
            }
        }

        for (index in toStart) {
            val item = feed.getOrNull(index) ?: continue
            reporter.preloadScheduled(item.id, index, index - centerIndex, "disk")
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

        scheduledPrefetchTargets = desired
    }

    private fun cancelPrefetch(reason: String) {
        val indices = scheduledPrefetchTargets.toList()
        for (index in indices) {
            feed.getOrNull(index)?.let { item ->
                cache.cancelPrefetch(item)
                reporter.preloadCanceled(item.id, index, reason)
            }
        }
        scheduledPrefetchTargets = emptySet()
        cache.cancelAll()
    }

    private fun prepareSlot(slot: PlayerSlot, index: Int, shouldPlay: Boolean) {
        val descriptor = feed[index]
        val item = buildPlayerItem(descriptor)
        item.preferredForwardBufferDuration = 1.0
        slot.index = index
        slot.player.replaceCurrentItemWithPlayerItem(item)
        if (shouldPlay) {
            slot.player.playImmediatelyAtRate(1.0F)
        } else {
            slot.player.pause()
        }
    }

    private fun attachIfBound(slot: PlayerSlot) {
        val index = slot.index ?: return
        val handle = surfaces[index]
        if (handle is IosVideoSurfaceHandle) {
            if (handle.controller.player != slot.player) {
                handle.controller.player = slot.player
            }
        }
    }

    private fun buildPlayerItem(descriptor: MediaDescriptor): AVPlayerItem {
        val cachedUrl = cache.cachedFileUrl(descriptor)
        val url = cachedUrl ?: NSURL.URLWithString(descriptor.uri) ?: return AVPlayerItem()
        val options: Map<Any?, *>? = if (cachedUrl == null && descriptor.headers.isNotEmpty()) {
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
        return AVPlayerItem(asset = asset)
    }

    private fun registerObservers() {
        val center = NSNotificationCenter.defaultCenter

        observers += center.addObserverForName(
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

        observers += center.addObserverForName(
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

        observers += center.addObserverForName(
            name = AVPlayerItemDidPlayToEndTimeNotification,
            `object` = null,
            queue = null,
        ) { notification ->
            val current = activeSlot.player.currentItem
            if (notification?.`object` == current) {
                val index = activeSlot.index ?: return@addObserverForName
                val item = feed.getOrNull(index) ?: return@addObserverForName
                reporter.playbackEnded(item.id, index)
                val replacement = buildPlayerItem(item)
                replacement.preferredForwardBufferDuration = 1.0
                activeSlot.player.replaceCurrentItemWithPlayerItem(replacement)
                activeSlot.player.playImmediatelyAtRate(1.0F)
                firstFramePendingIndex = index
                playStartMsById[item.id] = nowMs()
            }
        }
    }

    private fun startPolling() {
        pollTimer?.invalidate()
        pollTimer = NSTimer.scheduledTimerWithTimeInterval(
            interval = 0.2,
            repeats = true,
        ) {
            scope.launch {
                pollPlaybackState()
            }
        }
        NSRunLoop.mainRunLoop.addTimer(pollTimer!!, NSRunLoopCommonModes)
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun pollPlaybackState() {
        val index = activeSlot.index ?: return
        val item = feed.getOrNull(index) ?: return

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
                val start = playStartMsById[item.id] ?: nowMs()
                reporter.firstFrameRendered(item.id, index)
                reporter.timeToFirstFrame(item.id, index, nowMs() - start)
            }
        }

        val preparedIndex = preparedPendingIndex
        val prepared = preparedSlot
        if (prepared != null && preparedIndex != null && prepared.index == preparedIndex) {
            val preparedItem = prepared.player.currentItem
            if (preparedItem?.status == AVPlayerItemStatusReadyToPlay) {
                if (!preparedPrerollRequested) {
                    preparedPrerollRequested = true
                    prepared.player.prerollAtRate(1.0F) { _ ->
                        if (prepared.index == preparedIndex) {
                            prepared.player.pause()
                        }
                    }
                }
                val start = preparedStartMs ?: nowMs()
                reporter.preloadCompleted(feed[preparedIndex].id, preparedIndex, 0, nowMs() - start, false)
                preparedPendingIndex = null
                preparedStartMs = null
            }
            if (preparedItem?.status == AVPlayerItemStatusFailed) {
                reporter.preloadCanceled(feed[preparedIndex].id, preparedIndex, "error")
                preparedPendingIndex = null
                preparedStartMs = null
            }
        }
    }

    private data class PlayerSlot(
        val player: AVPlayer,
        var index: Int? = null,
    )
}
