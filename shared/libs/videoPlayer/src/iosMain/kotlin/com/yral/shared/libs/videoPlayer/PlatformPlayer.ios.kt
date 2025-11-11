package com.yral.shared.libs.videoPlayer

import com.yral.shared.libs.videoPlayer.util.isHlsUrl
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerActionAtItemEndNone
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerItemFailedToPlayToEndTimeErrorKey
import platform.AVFoundation.AVPlayerItemFailedToPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerItemStatusFailed
import platform.AVFoundation.AVPlayerItemStatusReadyToPlay
import platform.AVFoundation.AVPlayerTimeControlStatusPlaying
import platform.AVFoundation.AVPlayerTimeControlStatusWaitingToPlayAtSpecifiedRate
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.actionAtItemEnd
import platform.AVFoundation.addPeriodicTimeObserverForInterval
import platform.AVFoundation.asset
import platform.AVFoundation.automaticallyWaitsToMinimizeStalling
import platform.AVFoundation.canUseNetworkResourcesForLiveStreamingWhilePaused
import platform.AVFoundation.currentItem
import platform.AVFoundation.currentTime
import platform.AVFoundation.duration
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.preferredForwardBufferDuration
import platform.AVFoundation.rate
import platform.AVFoundation.removeTimeObserver
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.seekToTime
import platform.AVFoundation.timeControlStatus
import platform.AVFoundation.volume
import platform.CoreMedia.CMTime
import platform.CoreMedia.CMTimeGetSeconds
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.darwin.dispatch_get_main_queue
import kotlin.math.roundToLong

@Suppress("TooManyFunctions")
actual class PlatformPlayer {
    private var player: AVPlayer? = createPlayer()
    private var playbackSpeed: Float = DEFAULT_PLAYBACK_SPEED
    private val listeners = mutableSetOf<PlatformPlayerListener>()
    private var periodicObserver: Any? = null
    private var endObserver: Any? = null
    private var failureObserver: Any? = null
    private var lastDurationMs: Long? = null
    private var lastPlaybackState: PlatformPlaybackState? = null

    internal val nativePlayer: AVPlayer?
        get() = player

    actual fun play() {
        player?.let {
            it.play()
            if (playbackSpeed != DEFAULT_PLAYBACK_SPEED) {
                it.rate = playbackSpeed
            }
        }
        updateStateAndDuration()
    }

    actual fun pause() {
        player?.pause()
        updateStateAndDuration()
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun stop() {
        player?.let {
            it.pause()
            it.seekToTime(cmTime(positionMs = 0L))
        }
        notifyPlaybackState(PlatformPlaybackState.IDLE)
    }

    actual fun release() {
        player?.let {
            detachStateObservers()
            it.pause()
            it.replaceCurrentItemWithPlayerItem(null)
        }
        listeners.clear()
        player = null
        resetStateTracking()
    }

    actual fun clearMediaItems() {
        player?.replaceCurrentItemWithPlayerItem(null)
        resetStateTracking()
        unregisterItemObservers()
        notifyPlaybackState(PlatformPlaybackState.IDLE)
    }

    actual fun setMediaSource(source: Any) {
        val currentPlayer = player ?: return
        val (playerItem, isHlsSource) =
            when (source) {
                is AVPlayerItem -> source to source.isHlsItem()
                is NSURL -> AVPlayerItem(uRL = source) to (source.absoluteString?.let(::isHlsUrl) ?: false)
                is String -> resolveUrl(source)?.let { AVPlayerItem(uRL = it) to isHlsUrl(source) }
                else -> null
            } ?: (null to false)

        requireNotNull(playerItem) {
            "Unsupported media source type: ${source::class.simpleName}"
        }

        if (isHlsSource) {
            playerItem.canUseNetworkResourcesForLiveStreamingWhilePaused = true
            playerItem.preferredForwardBufferDuration = HLS_FORWARD_BUFFER_DURATION_SECONDS
            currentPlayer.automaticallyWaitsToMinimizeStalling = true
        } else {
            currentPlayer.automaticallyWaitsToMinimizeStalling = false
        }

        currentPlayer.replaceCurrentItemWithPlayerItem(playerItem)
        resetStateTracking()
        if (listeners.isNotEmpty()) {
            registerItemObservers(playerItem)
        }
        updateStateAndDuration()
    }

    actual fun prepare() {
        // AVPlayer automatically prepares as soon as an item is assigned.
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun seekTo(positionMs: Long) {
        player?.seekToTime(cmTime(positionMs))
        updateStateAndDuration()
    }

    actual fun setVolume(volume: Float) {
        player?.volume = volume
    }

    actual fun setPlaybackSpeed(speed: Float) {
        playbackSpeed = speed
        val current = player ?: return
        if (current.timeControlStatus == AVPlayerTimeControlStatusPlaying) {
            current.rate = playbackSpeed
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun currentPosition(): Long {
        val currentTime = player?.currentTime() ?: return 0L
        return cmTimeToMs(currentTime) ?: 0L
    }

    actual fun addListener(listener: PlatformPlayerListener) {
        listeners.add(listener)
        if (listeners.size == 1) {
            attachStateObservers()
        }
        val currentPlayer = player
        val currentItem = currentPlayer?.currentItem
        val duration = computeDurationMs(currentItem)
        if (duration != null) {
            listener.onDurationChanged(duration)
        }
        listener.onPlaybackStateChanged(determinePlaybackState(currentPlayer, currentItem))
    }

    actual fun removeListener(listener: PlatformPlayerListener) {
        if (listeners.remove(listener) && listeners.isEmpty()) {
            detachStateObservers()
        }
    }

    private fun createPlayer(): AVPlayer =
        AVPlayer().apply {
            actionAtItemEnd = AVPlayerActionAtItemEndNone
            automaticallyWaitsToMinimizeStalling = false
        }

    private fun resolveUrl(raw: String): NSURL? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        return when {
            trimmed.startsWith("http", ignoreCase = true) ||
                trimmed.startsWith("file://", ignoreCase = true) -> NSURL(string = trimmed)

            NSFileManager.defaultManager.fileExistsAtPath(path = trimmed) ->
                NSURL.fileURLWithPath(trimmed)

            else -> NSURL(string = trimmed)
        }
    }

    private fun AVPlayerItem.isHlsItem(): Boolean {
        val asset = asset
        if (asset is AVURLAsset) {
            val urlString = asset.URL?.absoluteString
            if (urlString != null) {
                return isHlsUrl(urlString)
            }
        }
        return false
    }

    @OptIn(ExperimentalForeignApi::class)
    @Suppress("MagicNumber")
    private fun attachStateObservers() {
        val currentPlayer = player ?: return
        if (periodicObserver == null) {
            val interval = cmTimeInterval(0.3)
            periodicObserver =
                currentPlayer.addPeriodicTimeObserverForInterval(
                    interval = interval,
                    queue = dispatch_get_main_queue(),
                ) { updateStateAndDuration() }
        }
        registerItemObservers(currentPlayer.currentItem)
        updateStateAndDuration()
    }

    private fun detachStateObservers() {
        val currentPlayer = player ?: return
        periodicObserver?.let {
            currentPlayer.removeTimeObserver(it)
            periodicObserver = null
        }
        unregisterItemObservers()
    }

    private fun registerItemObservers(item: AVPlayerItem?) {
        unregisterItemObservers()
        if (item == null) return

        val notificationCenter = NSNotificationCenter.defaultCenter
        endObserver =
            notificationCenter.addObserverForName(
                name = AVPlayerItemDidPlayToEndTimeNotification,
                `object` = item,
                queue = NSOperationQueue.mainQueue(),
            ) { _: NSNotification? ->
                notifyPlaybackState(PlatformPlaybackState.ENDED)
            }

        failureObserver =
            notificationCenter.addObserverForName(
                name = AVPlayerItemFailedToPlayToEndTimeNotification,
                `object` = item,
                queue = NSOperationQueue.mainQueue(),
            ) { notification: NSNotification? ->
                val avError = notification?.userInfo?.get(AVPlayerItemFailedToPlayToEndTimeErrorKey) as? NSError
                val fallbackError = item.error
                val message =
                    avError?.localizedDescription
                        ?: fallbackError?.localizedDescription
                        ?: "Playback error"
                val code = avError.toErrorCode() ?: fallbackError.toErrorCode()
                notifyError(message, code)
                notifyPlaybackState(PlatformPlaybackState.IDLE)
            }
    }

    private fun unregisterItemObservers() {
        val notificationCenter = NSNotificationCenter.defaultCenter
        endObserver?.let {
            notificationCenter.removeObserver(it)
            endObserver = null
        }
        failureObserver?.let {
            notificationCenter.removeObserver(it)
            failureObserver = null
        }
    }

    private fun updateStateAndDuration() {
        val currentPlayer = player ?: return
        val currentItem = currentPlayer.currentItem
        val duration = computeDurationMs(currentItem)

        if (duration != null && duration != lastDurationMs) {
            lastDurationMs = duration
            notifyDuration(duration)
        }

        if (currentItem?.status == AVPlayerItemStatusFailed) {
            val error = currentItem.error
            notifyError(
                error?.localizedDescription ?: "Playback error",
                error.toErrorCode(),
            )
            notifyPlaybackState(PlatformPlaybackState.IDLE)
            return
        }

        val state = determinePlaybackState(currentPlayer, currentItem)

        val shouldNotify =
            !(
                state == PlatformPlaybackState.READY &&
                    lastPlaybackState == PlatformPlaybackState.ENDED &&
                    duration != null &&
                    duration > 0 &&
                    currentPosition() >= duration
            )

        if (shouldNotify) {
            notifyPlaybackState(state)
        }
    }

    @Suppress("MaxLineLength")
    private fun determinePlaybackState(
        player: AVPlayer?,
        item: AVPlayerItem?,
    ): PlatformPlaybackState =
        when {
            player == null || item == null -> PlatformPlaybackState.IDLE
            player.timeControlStatus == AVPlayerTimeControlStatusWaitingToPlayAtSpecifiedRate -> PlatformPlaybackState.BUFFERING
            item.status == AVPlayerItemStatusReadyToPlay -> PlatformPlaybackState.READY
            else -> PlatformPlaybackState.BUFFERING
        }

    private fun notifyPlaybackState(state: PlatformPlaybackState) {
        if (lastPlaybackState == state) return
        lastPlaybackState = state
        if (listeners.isEmpty()) return
        listeners.forEach { it.onPlaybackStateChanged(state) }
    }

    private fun notifyDuration(durationMs: Long) {
        if (listeners.isEmpty()) return
        listeners.forEach { it.onDurationChanged(durationMs) }
    }

    private fun notifyError(
        message: String?,
        code: Int?,
    ) {
        if (listeners.isEmpty()) return
        val error = PlatformPlayerError(message = message, code = code)
        listeners.forEach { it.onPlayerError(error) }
    }

    private fun resetStateTracking() {
        lastDurationMs = null
        lastPlaybackState = null
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun cmTime(positionMs: Long): CValue<CMTime> =
        cValue {
            value = ((positionMs.toDouble() * TIME_SCALE) / MILLIS_IN_SECOND).roundToLong()
            timescale = TIME_SCALE.toInt()
            flags = 1u
            epoch = 0L
        }

    @OptIn(ExperimentalForeignApi::class)
    private fun cmTimeInterval(seconds: Double): CValue<CMTime> =
        cValue {
            value = (seconds * TIME_SCALE).roundToLong()
            timescale = TIME_SCALE.toInt()
            flags = 1u
            epoch = 0L
        }

    @OptIn(ExperimentalForeignApi::class)
    private fun computeDurationMs(item: AVPlayerItem?): Long? {
        val duration = item?.duration ?: return null
        return cmTimeToMs(duration)
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun cmTimeToMs(currentTime: CValue<CMTime>): Long? {
        val seconds = CMTimeGetSeconds(currentTime)
        return if (seconds.isNaN() || seconds.isInfinite()) {
            null
        } else {
            (seconds * MILLIS_IN_SECOND).roundToLong()
        }
    }

    private fun NSError?.toErrorCode(): Int? =
        this?.code?.let { code ->
            if (code in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) {
                code.toInt()
            } else {
                null
            }
        }

    companion object {
        private const val TIME_SCALE = 600L
        private const val MILLIS_IN_SECOND = 1000.0
        private const val DEFAULT_PLAYBACK_SPEED = 1f
        private const val HLS_FORWARD_BUFFER_DURATION_SECONDS = 5.0
    }
}
