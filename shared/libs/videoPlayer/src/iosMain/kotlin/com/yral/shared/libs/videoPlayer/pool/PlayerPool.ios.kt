@file:Suppress("EmptyFunctionBlock")

package com.yral.shared.libs.videoPlayer.pool

import com.yral.shared.libs.videoPlayer.PlatformPlayer
import com.yral.shared.libs.videoPlayer.model.PlayerData

/**
 * STUB implementation
 */
actual class PlayerPool actual constructor(
    maxPoolSize: Int,
) {
    actual suspend fun getPlayer(
        playerData: PlayerData,
        videoListener: VideoListener?,
    ): PlatformPlayer = PlatformPlayer()

    actual suspend fun releasePlayer(player: PlatformPlayer) {
    }

    actual fun dispose() {
    }

    actual fun onPlayBackStarted(playerData: PlayerData) {
    }

    actual fun onPlayBackStopped(playerData: PlayerData) {
    }
}
