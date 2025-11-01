package com.yral.shared.libs.videoPlayer.pool

import com.yral.shared.libs.videoPlayer.PlatformPlayer
import com.yral.shared.libs.videoPlayer.model.PlayerData

/**
 * Simplified multiplatform player pool for efficient video player resource management
 */
expect class PlayerPool(
    maxPoolSize: Int = 3,
) {
    suspend fun getPlayer(
        playerData: PlayerData,
        videoListener: VideoListener? = null,
    ): PlatformPlayer
    suspend fun releasePlayer(player: PlatformPlayer)
    fun dispose()
    fun onPlayBackStarted(playerData: PlayerData)
    fun onPlayBackStopped(playerData: PlayerData)
}

interface VideoListener {
    fun onSetupPlayer()
    fun onBuffer()
    fun onReady()
    fun onIdle()
    fun onEnd()
    fun onPlayerError()
    fun onPlayBackStarted()
    fun onPlayBackStopped()
}
