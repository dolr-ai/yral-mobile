package com.yral.shared.libs.videoPlayer

/**
 * Platform-specific player wrapper - simplified interface
 */
expect class PlatformPlayer {
    fun play()
    fun pause()
    fun stop()
    fun release()
    fun clearMediaItems()
    fun setMediaSource(source: Any)
    fun prepare()
    fun seekTo(positionMs: Long)
    fun setVolume(volume: Float)
    fun setPlaybackSpeed(speed: Float)

    fun currentPosition(): Long
}
