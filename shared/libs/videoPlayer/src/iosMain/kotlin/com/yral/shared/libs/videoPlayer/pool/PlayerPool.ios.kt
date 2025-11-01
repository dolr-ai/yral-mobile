@file:Suppress("EmptyFunctionBlock")

package com.yral.shared.libs.videoPlayer.pool

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

/**
 * STUB implementation
 */
actual class PlatformPlayer {
    actual fun play() {
    }

    actual fun pause() {
    }

    actual fun stop() {
    }

    actual fun release() {
    }

    actual fun clearMediaItems() {
    }

    actual fun setMediaSource(source: Any) {
    }

    actual fun prepare() {
    }

    actual fun seekTo(
        mediaItemIndex: Int,
        positionMs: Long,
    ) {
    }

    actual fun seekTo(positionMs: Long) {
    }

    actual fun setVolume(volume: Float) {
    }

    actual fun setPlaybackSpeed(speed: Float) {
    }

    actual fun currentPosition(): Long {
        TODO("Not yet implemented")
    }
}
