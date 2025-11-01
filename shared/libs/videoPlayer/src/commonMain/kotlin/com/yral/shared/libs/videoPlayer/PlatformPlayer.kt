package com.yral.shared.libs.videoPlayer

/**
 * Platform-specific player wrapper - simplified interface
 */
enum class PlatformPlaybackState {
    IDLE,
    BUFFERING,
    READY,
    ENDED,
}

class PlatformPlayerError(
    message: String? = null,
    cause: Throwable? = null,
    val code: Int? = null,
) : Exception(message, cause)

interface PlatformPlayerListener {
    fun onPlaybackStateChanged(state: PlatformPlaybackState) {}
    fun onDurationChanged(durationMs: Long) {}
    fun onPlayerError(error: PlatformPlayerError) {}
}

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
    fun addListener(listener: PlatformPlayerListener)
    fun removeListener(listener: PlatformPlayerListener)

    fun currentPosition(): Long
}
