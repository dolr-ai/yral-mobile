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
import platform.AVFoundation.rate
import platform.AVFoundation.reasonForWaitingToPlay
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
                detachSurface(stalePreparedIndex, slot.player)
                slot.index = null
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
            preparedSlot?.index = null
            preparedSlot?.source = null
            pendingDiskPrefetchCenterIndex = null
            return
        }

        alignment.nextActiveIndex?.let { targetIndex ->
            activeIndex = -1
            setActiveIndex(targetIndex)
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
        if (index == activeIndex && activeSlot.index == index) {
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

        if (preparedSlot?.index == index) {
            swapSlots()
        } else {
            prepareSlot(activeSlot, index, shouldPlay = true)
        }

        attachIfBound(activeSlot)
        activeSlot.player.playImmediatelyAtRate(1.0F)
        enforceSingleActivePlayback()

        schedulePreparedSlot(index)
        scheduleDiskPrefetch(index)
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
        if (isInteracting) {
            cancelPrefetch(reason = "interaction")
        } else {
            pendingDiskPrefetchCenterIndex?.let { centerIndex ->
                pendingDiskPrefetchCenterIndex = null
                scheduleDiskPrefetch(centerIndex)
            }
        }
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
        if (activeSlot.index in feed.indices) {
            activeSlot.player.playImmediatelyAtRate(1.0F)
            enforceSingleActivePlayback()
        }
    }

    override fun onAppBackground() {
        activeSlot.player.pause()
        preparedSlot?.player?.pause()
        cancelPrefetch(reason = "background")
        pendingDiskPrefetchCenterIndex = null
    }

    override fun release() {
        pollTimer?.invalidate()
        pollTimer = null
        observers.forEach { NSNotificationCenter.defaultCenter.removeObserver(it) }
        observers.clear()
        activeSlot.player.pause()
        preparedSlot?.player?.pause()
        cancelPrefetch(reason = "release")
        pendingDiskPrefetchCenterIndex = null
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
                prepareSlot(prepared, nextIndex, shouldPlay = false)
            }
            prepared.player.pause()
            preparedScheduler.setStartTime(nowMs())
            enforceSingleActivePlayback()
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
        slot.player.replaceCurrentItemWithPlayerItem(item)
        if (shouldPlay) {
            slot.player.playImmediatelyAtRate(1.0F)
        } else {
            slot.player.pause()
        }
        enforceSingleActivePlayback()
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
            prepared.player.pause()
            val preparedIndex = prepared.index
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
                val current = activeSlot.player.currentItem
                if (notification?.`object` == current) {
                    val index = activeSlot.index ?: return@addObserverForName
                    val item = feed.getOrNull(index) ?: return@addObserverForName
                    reporter.playbackEnded(item.id, index)
                    val replacementBuild = buildPlayerItem(item, index)
                    val replacement = replacementBuild.item
                    replacement.preferredForwardBufferDuration = 1.0
                    activeSlot.source = replacementBuild.source
                    activeSlot.player.replaceCurrentItemWithPlayerItem(replacement)
                    activeSlot.player.playImmediatelyAtRate(1.0F)
                    enforceSingleActivePlayback()
                    firstFramePendingIndex = index
                    firstFrameStartupWatchdog.start(index, nowMs())
                    playStartMsById[item.id] = nowMs()
                }
            }
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

    @Suppress("CyclomaticComplexMethod", "MagicNumber")
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
                firstFrameStartupWatchdog.clear(index)
                endStartupStall(index)
                val start = playStartMsById[item.id] ?: nowMs()
                reporter.firstFrameRendered(item.id, index)
                reporter.timeToFirstFrame(item.id, index, nowMs() - start)
            }
        }

        recoverFirstFrameStartupIfNeeded(index, item)

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
                val replacementBuild = buildPlayerItem(item, index)
                val replacement = replacementBuild.item
                replacement.preferredForwardBufferDuration = 1.0
                activeSlot.source = replacementBuild.source
                activeSlot.player.replaceCurrentItemWithPlayerItem(replacement)
                attachIfBound(activeSlot)
                activeSlot.player.playImmediatelyAtRate(1.0F)
                enforceSingleActivePlayback()
                firstFramePendingIndex = index
                playStartMsById[item.id] = nowMs()
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

    private data class PlayerSlot(
        val player: AVPlayer,
        var index: Int? = null,
        var source: PlaybackSourceDiagnostics? = null,
    )

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
        private const val MAX_SCROLL_SETTLED_PREFETCH_STARTS = 1
    }
}
