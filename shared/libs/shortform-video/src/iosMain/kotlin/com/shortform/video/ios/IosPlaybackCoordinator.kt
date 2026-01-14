package com.shortform.video.ios

import com.shortform.video.CoordinatorDeps
import com.shortform.video.MediaDescriptor
import com.shortform.video.PlaybackCoordinator
import com.shortform.video.VideoSurfaceHandle
import com.shortform.video.ui.IosVideoSurfaceHandle
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
import platform.AVFoundation.AVPlayerItemStatusReadyToPlay
import platform.AVFoundation.AVPlayerItemStatusFailed
import platform.AVFoundation.AVPlayerTimeControlStatusPlaying
import platform.AVFoundation.AVPlayerTimeControlStatusWaitingToPlayAtSpecifiedRate
import platform.AVFoundation.AVURLAsset
import platform.CoreMedia.CMTimeGetSeconds
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSObjectProtocol
import platform.Foundation.NSTimer
import platform.Foundation.NSURL
import platform.Foundation.scheduledTimerWithTimeInterval
import platform.AVFoundation.AVURLAssetHTTPHeaderFieldsKey
import platform.Foundation.NSRunLoop
import platform.Foundation.NSRunLoopCommonModes

fun createIosPlaybackCoordinator(
    deps: CoordinatorDeps = CoordinatorDeps(),
): PlaybackCoordinator = IosPlaybackCoordinator(deps)

private class IosPlaybackCoordinator(
    private val deps: CoordinatorDeps,
) : PlaybackCoordinator {
    private val analytics = deps.analytics
    private val nowMs = deps.nowMs

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var pollTimer: NSTimer? = null

    private val playerA = AVPlayer()
    private val playerB = AVPlayer()
    private var activeSlot = PlayerSlot(playerA)
    private var preparedSlot = PlayerSlot(playerB)

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

    private val observers = mutableListOf<NSObjectProtocol>()

    init {
        registerObservers()
        startPolling()
    }

    override fun setFeed(items: List<MediaDescriptor>) {
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

        analytics.event("feed_item_impression", mapOf("id" to item.id, "index" to index))
        analytics.event("play_start_request", mapOf("id" to item.id, "index" to index, "reason" to "activeIndex"))
        playStartMsById[item.id] = nowMs()
        firstFramePendingIndex = index

        if (preparedSlot.index == index) {
            swapSlots()
        } else {
            prepareSlot(activeSlot, index, shouldPlay = true)
        }

        attachIfBound(activeSlot)
        activeSlot.player.play()

        schedulePreparedSlot(index)
    }

    override fun setScrollHint(predictedIndex: Int, velocity: Float?) {
        if (predictedIndex !in feed.indices) return
        if (predictedIndex == this.predictedIndex) return
        this.predictedIndex = predictedIndex
    }

    override fun bindSurface(index: Int, surface: VideoSurfaceHandle) {
        surfaces[index] = surface
        if (activeSlot.index == index) {
            attachIfBound(activeSlot)
        } else if (preparedSlot.index == index) {
            attachIfBound(preparedSlot)
        }
    }

    override fun unbindSurface(index: Int) {
        val handle = surfaces.remove(index)
        if (handle is IosVideoSurfaceHandle) {
            if (activeSlot.index == index && handle.playerLayer.player == activeSlot.player) {
                handle.playerLayer.player = null
            }
            if (preparedSlot.index == index && handle.playerLayer.player == preparedSlot.player) {
                handle.playerLayer.player = null
            }
        }
    }

    override fun onAppForeground() {
        if (activeSlot.index in feed.indices) {
            activeSlot.player.play()
        }
    }

    override fun onAppBackground() {
        activeSlot.player.pause()
        preparedSlot.player.pause()
    }

    override fun release() {
        pollTimer?.invalidate()
        pollTimer = null
        observers.forEach { NSNotificationCenter.defaultCenter.removeObserver(it) }
        observers.clear()
        activeSlot.player.pause()
        preparedSlot.player.pause()
        scope.cancel()
    }

    private fun schedulePreparedSlot(activeIndex: Int) {
        val nextIndex = activeIndex + 1
        if (nextIndex in feed.indices) {
            if (preparedSlot.index != nextIndex) {
                prepareSlot(preparedSlot, nextIndex, shouldPlay = false)
                preparedPendingIndex = nextIndex
                preparedStartMs = nowMs()
                analytics.event(
                    "preload_scheduled",
                    mapOf("id" to feed[nextIndex].id, "index" to nextIndex, "distance" to 1, "mode" to "prepared"),
                )
            }
        }
    }

    private fun prepareSlot(slot: PlayerSlot, index: Int, shouldPlay: Boolean) {
        val descriptor = feed[index]
        val item = buildPlayerItem(descriptor)
        slot.index = index
        slot.player.replaceCurrentItemWithPlayerItem(item)
        if (shouldPlay) {
            slot.player.play()
        } else {
            slot.player.pause()
        }
    }

    private fun swapSlots() {
        val previousActive = activeSlot
        activeSlot = preparedSlot
        preparedSlot = previousActive
    }

    private fun attachIfBound(slot: PlayerSlot) {
        val index = slot.index ?: return
        val handle = surfaces[index]
        if (handle is IosVideoSurfaceHandle) {
            if (handle.playerLayer.player != slot.player) {
                handle.playerLayer.player = slot.player
            }
        }
    }

    private fun buildPlayerItem(descriptor: MediaDescriptor): AVPlayerItem {
        val url = NSURL.URLWithString(descriptor.uri) ?: return AVPlayerItem()
        val options = if (descriptor.headers.isNotEmpty()) {
            mapOf(AVURLAssetHTTPHeaderFieldsKey to descriptor.headers)
        } else {
            null
        }
        val asset = AVURLAsset(uRL = url, options = options)
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
                analytics.event(
                    "playback_error",
                    mapOf("id" to item.id, "index" to index, "category" to "failed", "code" to "ios"),
                )
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
                analytics.event(
                    "rebuffer_start",
                    mapOf("id" to item.id, "index" to index, "reason" to "stalled"),
                )
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
                analytics.event("playback_ended", mapOf("id" to item.id, "index" to index))
                val replacement = buildPlayerItem(item)
                activeSlot.player.replaceCurrentItemWithPlayerItem(replacement)
                activeSlot.player.play()
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

    private fun pollPlaybackState() {
        val index = activeSlot.index ?: return
        val item = feed.getOrNull(index) ?: return

        val status = activeSlot.player.timeControlStatus
        if (status == AVPlayerTimeControlStatusWaitingToPlayAtSpecifiedRate && !rebuffering) {
            rebuffering = true
            rebufferStartMs = nowMs()
            analytics.event(
                "rebuffer_start",
                mapOf("id" to item.id, "index" to index, "reason" to "buffering"),
            )
        }

        if (status == AVPlayerTimeControlStatusPlaying && rebuffering) {
            rebuffering = false
            val start = rebufferStartMs
            if (start != null) {
                analytics.event(
                    "rebuffer_end",
                    mapOf("id" to item.id, "index" to index, "reason" to "buffering"),
                )
                analytics.timing(
                    "rebuffer_total_ms",
                    nowMs() - start,
                    mapOf("id" to item.id, "index" to index),
                )
            }
            rebufferStartMs = null
        }

        if (firstFramePendingIndex == index) {
            val seconds = CMTimeGetSeconds(activeSlot.player.currentTime())
            if (seconds > 0.01) {
                firstFramePendingIndex = null
                val start = playStartMsById[item.id] ?: nowMs()
                analytics.event("first_frame_rendered", mapOf("id" to item.id, "index" to index))
                analytics.timing("time_to_first_frame_ms", nowMs() - start, mapOf("id" to item.id, "index" to index))
            }
        }

        val preparedIndex = preparedPendingIndex
        if (preparedIndex != null && preparedSlot.index == preparedIndex) {
            val preparedItem = preparedSlot.player.currentItem
            if (preparedItem?.status == AVPlayerItemStatusReadyToPlay) {
                val start = preparedStartMs ?: nowMs()
                analytics.event(
                    "preload_completed",
                    mapOf("id" to feed[preparedIndex].id, "index" to preparedIndex, "bytes" to 0, "ms" to (nowMs() - start), "fromCache" to false),
                )
                preparedPendingIndex = null
                preparedStartMs = null
            }
            if (preparedItem?.status == AVPlayerItemStatusFailed) {
                analytics.event(
                    "preload_canceled",
                    mapOf("id" to feed[preparedIndex].id, "index" to preparedIndex, "reason" to "error"),
                )
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
