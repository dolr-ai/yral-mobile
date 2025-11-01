package com.yral.shared.libs.videoPlayer

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource

/**
 * Simplified Android PlatformPlayer wrapping ExoPlayer
 */
actual class PlatformPlayer(
    private val exoPlayer: ExoPlayer,
) {
    private val listenerMap = mutableMapOf<PlatformPlayerListener, Player.Listener>()

    actual fun play() {
        exoPlayer.playWhenReady = true
    }

    actual fun pause() {
        exoPlayer.playWhenReady = false
    }

    actual fun release() {
        exoPlayer.release()
    }

    actual fun stop() {
        exoPlayer.stop()
    }

    actual fun clearMediaItems() {
        exoPlayer.clearMediaItems()
    }

    @OptIn(UnstableApi::class)
    actual fun setMediaSource(source: Any) {
        exoPlayer.setMediaSource(source as MediaSource)
    }

    actual fun prepare() {
        exoPlayer.prepare()
    }

    actual fun seekTo(positionMs: Long) {
        if (exoPlayer.currentPosition != positionMs) {
            exoPlayer.seekTo(positionMs)
        }
    }

    // Internal access to ExoPlayer for Android-specific operations like performance monitoring
    internal val internalExoPlayer: ExoPlayer = exoPlayer

    actual fun setVolume(volume: Float) {
        exoPlayer.volume = volume
    }

    actual fun setPlaybackSpeed(speed: Float) {
        if (exoPlayer.playbackParameters.speed != speed) {
            exoPlayer.setPlaybackSpeed(speed)
        }
    }

    actual fun currentPosition(): Long = exoPlayer.currentPosition

    actual fun addListener(listener: PlatformPlayerListener) {
        val delegate =
            object : Player.Listener {
                override fun onEvents(
                    player: Player,
                    events: Player.Events,
                ) {
                    val duration = player.duration
                    if (duration != C.TIME_UNSET) {
                        listener.onDurationChanged(duration.coerceAtLeast(0L))
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    listener.onPlaybackStateChanged(playbackState.toPlatformPlaybackState())
                }

                override fun onPlayerError(error: PlaybackException) {
                    listener.onPlayerError(
                        PlatformPlayerError(
                            message = error.message,
                            cause = error.cause,
                            code = error.errorCode,
                        ),
                    )
                }
            }

        listenerMap.put(listener, delegate)?.let { previous ->
            exoPlayer.removeListener(previous)
        }
        exoPlayer.addListener(delegate)

        listener.onPlaybackStateChanged(exoPlayer.playbackState.toPlatformPlaybackState())
        val currentDuration = exoPlayer.duration
        if (currentDuration != C.TIME_UNSET) {
            listener.onDurationChanged(currentDuration.coerceAtLeast(0L))
        }
    }

    actual fun removeListener(listener: PlatformPlayerListener) {
        val delegate = listenerMap.remove(listener) ?: return
        exoPlayer.removeListener(delegate)
    }

    private fun Int.toPlatformPlaybackState(): PlatformPlaybackState =
        when (this) {
            Player.STATE_BUFFERING -> PlatformPlaybackState.BUFFERING
            Player.STATE_READY -> PlatformPlaybackState.READY
            Player.STATE_ENDED -> PlatformPlaybackState.ENDED
            Player.STATE_IDLE -> PlatformPlaybackState.IDLE
            else -> PlatformPlaybackState.IDLE
        }

    fun getPlayBackState(): Int = exoPlayer.playbackState

    @OptIn(UnstableApi::class)
    fun getVideoFormat(): Format? = exoPlayer.videoFormat
}
