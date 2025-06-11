package com.yral.shared.libs.videoPlayer.pool

/**
 * Simplified multiplatform player pool for efficient video player resource management
 */
expect class PlayerPool(
    maxPoolSize: Int = 3,
) {
    suspend fun getPlayer(url: String): PlatformPlayer
    suspend fun releasePlayer(player: PlatformPlayer)
    suspend fun startFirstFrameTraceForUrl(url: String)
    fun dispose()
}

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
    fun seekTo(
        mediaItemIndex: Int,
        positionMs: Long,
    )
}
