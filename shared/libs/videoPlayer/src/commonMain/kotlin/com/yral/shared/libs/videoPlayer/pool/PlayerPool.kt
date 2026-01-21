package com.yral.shared.libs.videoPlayer.pool

import com.yral.shared.libs.videoPlayer.PlatformPlaybackState
import com.yral.shared.libs.videoPlayer.PlatformPlayer
import com.yral.shared.libs.videoPlayer.PlatformPlayerError
import com.yral.shared.libs.videoPlayer.PlatformPlayerListener
import com.yral.shared.libs.videoPlayer.model.PlayerData
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Simplified multiplatform player pool for efficient video player resource management
 */
class PlayerPool(
    private val platformPlayerFactory: PlatformPlayerFactory,
    private val platformMediaSourceFactory: PlatformMediaSourceFactory,
    private val maxPoolSize: Int = 3,
) {
    private val pool = mutableListOf<PooledPlayer>()
    private val mutex = Mutex()

    private data class PooledPlayer(
        val platformPlayer: PlatformPlayer,
        var isInUse: Boolean = false,
        var currentUrl: String? = null,
        var internalListener: PlatformPlayerListener? = null,
        var externalListener: VideoListener?,
    )

    suspend fun getPlayer(
        playerData: PlayerData,
        videoListener: VideoListener?,
    ): PlatformPlayer =
        mutex.withLock {
            // FIRST: Check if we already have a player for this URL (active or recently released)
            // This handles the transition from "next card" to "front card" without restart
            val existingPlayer =
                pool.find {
                    it.currentUrl == playerData.url
                }
            if (existingPlayer != null) {
                // Just update the listener and mark in use, don't reset the player
                existingPlayer.isInUse = true
                existingPlayer.externalListener = videoListener
                // Mark thumbnail hidden since we're reusing an existing player
                // This prevents thumbnail flash on composable recreation during card transition
                markThumbnailHidden(playerData.url)
                return@withLock existingPlayer.platformPlayer
            }

            // Find available player or create new one
            val availablePlayer = pool.find { !it.isInUse }

            if (availablePlayer != null) {
                setupPlayerForUrl(availablePlayer, playerData)
                availablePlayer.externalListener = videoListener
                availablePlayer.platformPlayer
            } else if (pool.size < maxPoolSize) {
                val platformPlayer = platformPlayerFactory.createPlayer()
                val pooledPlayer =
                    PooledPlayer(
                        platformPlayer = platformPlayer,
                        externalListener = videoListener,
                    )
                pool.add(pooledPlayer)
                setupPlayerForUrl(pooledPlayer, playerData)
                platformPlayer
            } else {
                // Reclaim least recently used player (first in pool)
                val playerToReclaim = pool.removeAt(0)
                playerToReclaim.externalListener = videoListener
                // Mark previous usage as released
                playerToReclaim.isInUse = false
                // Setup for new URL and mark as in use
                setupPlayerForUrl(playerToReclaim, playerData)
                // Move to end of pool (most recently used)
                pool.add(playerToReclaim)
                playerToReclaim.platformPlayer
            }
        }

    suspend fun releasePlayer(player: PlatformPlayer): Unit =
        mutex.withLock {
            pool.find { it.platformPlayer == player }?.let { pooledPlayer ->
                pooledPlayer.isInUse = false
                pooledPlayer.currentUrl = null
                pooledPlayer.platformPlayer.pause()
                pooledPlayer.platformPlayer.stop()
                pooledPlayer.platformPlayer.clearMediaItems()
//                pooledPlayer.externalListener = null
                cleanup(pooledPlayer)
            }
        }

    /**
     * Mark a player as available for reuse without full cleanup.
     * Non-suspend version for use in DisposableEffect.onDispose
     */
    fun markPlayerAvailable(player: PlatformPlayer) {
        pool.find { it.platformPlayer == player }?.let { pooledPlayer ->
            pooledPlayer.isInUse = false
        }
    }

    /**
     * Check if there's an active player for this URL that is currently playing.
     * Used to determine if thumbnail should be shown during composable recreation.
     */
    fun isPlayerActiveForUrl(url: String): Boolean {
        return pool.any { it.currentUrl == url && it.platformPlayer.isPlaying() }
    }

    // Track URLs that have had their thumbnails hidden (video became ready)
    // This persists across composable recreations
    private val thumbnailHiddenUrls = mutableSetOf<String>()

    /**
     * Mark that a URL's thumbnail has been hidden (video is ready).
     * Once hidden, we never show the thumbnail again for this URL.
     */
    fun markThumbnailHidden(url: String) {
        thumbnailHiddenUrls.add(url)
    }

    /**
     * Check if the thumbnail should be shown for this URL.
     * Returns false if the thumbnail was already hidden or player is active.
     */
    fun shouldShowThumbnail(url: String): Boolean {
        if (url in thumbnailHiddenUrls) return false
        if (isPlayerActiveForUrl(url)) return false
        return true
    }

    /**
     * Clear thumbnail state for a URL (e.g., when video is no longer in view).
     */
    fun clearThumbnailState(url: String) {
        thumbnailHiddenUrls.remove(url)
    }

    private fun cleanup(pooledPlayer: PooledPlayer) {
        pooledPlayer.internalListener?.let { listener ->
            pooledPlayer.platformPlayer.removeListener(listener)
            pooledPlayer.internalListener = null
        }
    }

    private fun setupPlayerForUrl(
        pooledPlayer: PooledPlayer,
        playerData: PlayerData,
    ) {
        // If already set up for this URL, don't recreate traces
        if (pooledPlayer.currentUrl == playerData.url && pooledPlayer.isInUse) {
            return
        }

        // Clean up previous traces and listeners
        cleanup(pooledPlayer)

        pooledPlayer.isInUse = true
        pooledPlayer.currentUrl = playerData.url

        // Reset player state completely
        pooledPlayer.platformPlayer.stop()
        pooledPlayer.platformPlayer.clearMediaItems()
        pooledPlayer.platformPlayer.pause() // Ensure consistent initial state

        // Set up listeners
        pooledPlayer.externalListener?.onSetupPlayer()
        setupInternalListener(pooledPlayer)

        val mediaSource = platformMediaSourceFactory.createMediaSource(playerData.url)

        pooledPlayer.platformPlayer.setMediaSource(mediaSource)
        pooledPlayer.platformPlayer.seekTo(0)
        pooledPlayer.platformPlayer.prepare()
    }

    private fun setupInternalListener(pooledPlayer: PooledPlayer) {
        val listener =
            createPooledPlayerPerformanceListener(
                onStateChange = { playbackState ->
                    when (playbackState) {
                        PlatformPlaybackState.BUFFERING -> {
                            pooledPlayer.externalListener?.onBuffer()
                        }

                        PlatformPlaybackState.READY -> {
                            pooledPlayer.externalListener?.onReady()
                        }

                        PlatformPlaybackState.IDLE -> {
                            pooledPlayer.externalListener?.onIdle()
                        }

                        PlatformPlaybackState.ENDED -> {
                            pooledPlayer.externalListener?.onEnd()
                        }
                    }
                },
                onError = {
                    pooledPlayer.externalListener?.onPlayerError()
                },
            )

        pooledPlayer.internalListener = listener
        pooledPlayer.platformPlayer.addListener(listener)
    }

    private fun createPooledPlayerPerformanceListener(
        onStateChange: (PlatformPlaybackState) -> Unit,
        onError: (PlatformPlayerError) -> Unit,
    ): PlatformPlayerListener =
        object : PlatformPlayerListener {
            override fun onPlaybackStateChanged(state: PlatformPlaybackState) {
                onStateChange(state)
            }

            override fun onPlayerError(error: PlatformPlayerError) {
                error.printStackTrace()
                onError(error)
            }
        }

    fun dispose() {
        pool.forEach { pooledPlayer ->
            cleanup(pooledPlayer)
            pooledPlayer.platformPlayer.release()
        }
        pool.clear()
    }

    suspend fun onPlayBackStarted(playerData: PlayerData) =
        mutex.withLock {
            val pooledPlayer = pool.find { it.currentUrl == playerData.url }
            pooledPlayer?.externalListener?.onPlayBackStarted()
        }

    suspend fun onPlayBackStopped(playerData: PlayerData) =
        mutex.withLock {
            val pooledPlayer = pool.find { it.currentUrl == playerData.url }
            pooledPlayer?.externalListener?.onPlayBackStopped()
        }
}

interface PlatformPlayerFactory {
    fun createPlayer(): PlatformPlayer
}

interface PlatformMediaSourceFactory {
    fun createMediaSource(url: String): Any
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
