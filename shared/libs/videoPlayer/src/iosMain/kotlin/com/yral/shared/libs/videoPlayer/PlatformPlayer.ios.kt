package com.yral.shared.libs.videoPlayer

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerActionAtItemEndNone
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerTimeControlStatusPlaying
import platform.AVFoundation.actionAtItemEnd
import platform.AVFoundation.automaticallyWaitsToMinimizeStalling
import platform.AVFoundation.currentTime
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.rate
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.seekToTime
import platform.AVFoundation.timeControlStatus
import platform.AVFoundation.volume
import platform.CoreMedia.CMTime
import platform.CoreMedia.CMTimeGetSeconds
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import kotlin.math.roundToLong

actual class PlatformPlayer {
    private var player: AVPlayer? = createPlayer()
    private var playbackSpeed: Float = DEFAULT_PLAYBACK_SPEED

    internal val nativePlayer: AVPlayer?
        get() = player

    actual fun play() {
        player?.let {
            it.play()
            if (playbackSpeed != DEFAULT_PLAYBACK_SPEED) {
                it.rate = playbackSpeed
            }
        }
    }

    actual fun pause() {
        player?.pause()
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun stop() {
        player?.let {
            it.pause()
            it.seekToTime(cmTime(positionMs = 0L))
        }
    }

    actual fun release() {
        player?.let {
            it.pause()
            it.replaceCurrentItemWithPlayerItem(null)
        }
        player = null
    }

    actual fun clearMediaItems() {
        player?.replaceCurrentItemWithPlayerItem(null)
    }

    actual fun setMediaSource(source: Any) {
        player?.let {
            val playerItem =
                when (source) {
                    is AVPlayerItem -> source
                    is NSURL -> AVPlayerItem(uRL = source)
                    is String -> resolveUrl(source)?.let { AVPlayerItem(uRL = it) }
                    else -> null
                }

            requireNotNull(playerItem) {
                "Unsupported media source type: ${source::class.simpleName}"
            }

            it.replaceCurrentItemWithPlayerItem(playerItem)
        }
    }

    actual fun prepare() {
        // AVPlayer automatically prepares as soon as an item is assigned.
        // Nothing explicit required for the current simplified wrapper.
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun seekTo(positionMs: Long) {
        player?.seekToTime(cmTime(positionMs))
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
        val seconds = CMTimeGetSeconds(currentTime)
        return if (seconds.isNaN() || seconds.isInfinite()) {
            0L
        } else {
            (seconds * MILLIS_IN_SECOND).roundToLong()
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
                NSURL.fileURLWithPath(
                    trimmed,
                )

            else -> NSURL(string = trimmed)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun cmTime(positionMs: Long): CValue<CMTime> =
        cValue {
            value = ((positionMs.toDouble() * TIME_SCALE) / MILLIS_IN_SECOND).roundToLong()
            timescale = TIME_SCALE.toInt()
            flags = 1u
            epoch = 0L
        }

    companion object {
        private const val TIME_SCALE = 600L
        private const val MILLIS_IN_SECOND = 1000.0
        private const val DEFAULT_PLAYBACK_SPEED = 1f
    }
}
