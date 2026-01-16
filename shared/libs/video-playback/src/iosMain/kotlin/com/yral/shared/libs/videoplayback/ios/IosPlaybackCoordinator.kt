package com.yral.shared.libs.videoplayback.ios

import com.yral.shared.libs.videoplayback.CoordinatorDeps
import com.yral.shared.libs.videoplayback.MediaDescriptor
import com.yral.shared.libs.videoplayback.PlaybackCoordinator
import com.yral.shared.libs.videoplayback.VideoSurfaceHandle
import com.yral.shared.libs.videoplayback.PreloadEventScheduler
import com.yral.shared.libs.videoplayback.PreparedSlotScheduler
import com.yral.shared.libs.videoplayback.PlaybackProgress
import com.yral.shared.libs.videoplayback.PlaybackProgressTicker
import com.yral.shared.libs.videoplayback.ui.IosVideoSurfaceHandle
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
import platform.AVFoundation.duration
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
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

fun createIosPlaybackCoordinator(
    deps: CoordinatorDeps = CoordinatorDeps(),
): PlaybackCoordinator = IosPlaybackCoordinator(deps)

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

    private val playStartMsById = mutableMapOf<String, Long>()
    private var firstFramePendingIndex: Int? = null

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
        cancelPrefetch(reason = "feed_update")
        preparedScheduler.reset("feed_update") { feed.getOrNull(it)?.id }
        feed = items
        if (activeIndex >= items.size) {
            setActiveIndex(items.lastIndex)
        }
    }

    override fun appendFeed(items: List<MediaDescriptor>) {
        if (items.isEmpty()) return
        feed = feed + items
        if (activeIndex == -1 && feed.isNotEmpty()) {
            setActiveIndex(0)
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
        preparedScheduler.reset("release") { feed.getOrNull(it)?.id }
        progressTicker.stop()
        scope.cancel()
    }

    private fun schedulePreparedSlot(activeIndex: Int) {
        val prepared = preparedSlot ?: return
        preparedScheduler.schedule(activeIndex, feed.size, { feed.getOrNull(it)?.id }) { nextIndex ->
            if (prepared.index != nextIndex) {
                prepareSlot(prepared, nextIndex, shouldPlay = false)
            }
            preparedScheduler.setStartTime(nowMs())
        }
    }

    private fun swapSlots() {
        val prepared = preparedSlot ?: return
        val previousActive = activeSlot
        activeSlot = prepared
        preparedSlot = previousActive
        preparedScheduler.clearOnSwap()
    }

    private fun scheduleDiskPrefetch(centerIndex: Int) {
        val result = preloadScheduler.update(centerIndex, feed.size) { feed.getOrNull(it)?.id }
        for (index in result.toCancel) {
            feed.getOrNull(index)?.let { item ->
                cache.cancelPrefetch(item)
            }
        }

        for (index in result.toStart) {
            if (index !in result.window.disk) continue
            val item = feed.getOrNull(index) ?: continue
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
    }

    private fun prepareSlot(slot: PlayerSlot, index: Int, shouldPlay: Boolean) {
        val descriptor = feed[index]
        val item = buildPlayerItem(descriptor)
        item.preferredForwardBufferDuration = 1.0
        val previousIndex = slot.index
        if (previousIndex != null && previousIndex != index) {
            detachSurface(previousIndex, slot.player)
        }
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
                handle.playerState.value = slot.player
            }
        }
    }

    private fun detachSurface(index: Int, player: AVPlayer) {
        val handle = surfaces[index] as? IosVideoSurfaceHandle ?: return
        if (handle.controller.player == player) {
            handle.controller.player = null
            handle.playerState.value = null
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

        val prepared = preparedSlot
        if (prepared != null) {
            val preparedItem = prepared.player.currentItem
            val preparedIndex = prepared.index
            if (preparedIndex != null && preparedItem?.status == AVPlayerItemStatusReadyToPlay) {
                preparedScheduler.markReady(
                    index = preparedIndex,
                    nowMs = nowMs(),
                    idAt = { feed.getOrNull(it)?.id },
                ) {
                    prepared.player.prerollAtRate(1.0F) { _ ->
                        if (prepared.index == preparedIndex) {
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

    private data class PlayerSlot(
        val player: AVPlayer,
        var index: Int? = null,
    )

    @OptIn(ExperimentalForeignApi::class)
    private fun activeProgress(): PlaybackProgress? {
        val index = activeSlot.index ?: return null
        val item = feed.getOrNull(index) ?: return null
        val status = activeSlot.player.timeControlStatus
        if (status != AVPlayerTimeControlStatusPlaying) return null
        val currentSeconds = CMTimeGetSeconds(activeSlot.player.currentTime())
        val durationSeconds = activeSlot.player.currentItem?.duration?.let { CMTimeGetSeconds(it) }
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
}
