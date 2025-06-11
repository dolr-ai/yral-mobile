package com.yral.shared.libs.videoPlayer.pool

import androidx.annotation.OptIn
import androidx.media3.common.Format
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

    actual fun seekTo(
        mediaItemIndex: Int,
        positionMs: Long,
    ) {
        exoPlayer.seekTo(mediaItemIndex, positionMs)
    }

    fun addListener(listener: Player.Listener) {
        exoPlayer.addListener(listener)
    }

    fun removeListener(listener: Player.Listener) {
        exoPlayer.removeListener(listener)
    }

    fun getPlayBackState(): Int = exoPlayer.playbackState

    @OptIn(UnstableApi::class)
    fun getVideoFormat(): Format? = exoPlayer.videoFormat

    // Internal access to ExoPlayer for Android-specific operations like performance monitoring
    internal val internalExoPlayer: ExoPlayer = exoPlayer
}
