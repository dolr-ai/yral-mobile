package com.yral.shared.libs.videoPlayer.pool

import android.content.Context
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.libs.videoPlayer.PlatformPlayer
import com.yral.shared.libs.videoPlayer.createHlsMediaSource
import com.yral.shared.libs.videoPlayer.createProgressiveMediaSource
import com.yral.shared.libs.videoPlayer.model.PlayerData
import com.yral.shared.libs.videoPlayer.util.isHlsUrl
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Android implementation of PlayerPool using ExoPlayer with integrated performance tracing
 */
actual class PlayerPool(
    private val context: Context,
    private val maxPoolSize: Int = 3,
) {
    actual constructor(maxPoolSize: Int) : this(
        throw YralException("Android PlayerPool requires context. Use rememberPlayerPool() instead."),
        maxPoolSize,
    )

    private val pool = mutableListOf<PooledExoPlayer>()
    private val mutex = Mutex()

    private data class PooledExoPlayer(
        val platformPlayer: PlatformPlayer,
        var isInUse: Boolean = false,
        var currentUrl: String? = null,
        var internalListener: Player.Listener? = null,
        var externalListener: VideoListener?,
    )

    actual suspend fun getPlayer(
        playerData: PlayerData,
        videoListener: VideoListener?,
    ): PlatformPlayer =
        mutex.withLock {
            // Find available player or create new one
            val availablePlayer = pool.find { !it.isInUse }

            if (availablePlayer != null) {
                setupPlayerForUrl(availablePlayer, playerData)
                availablePlayer.externalListener = videoListener
                availablePlayer.platformPlayer
            } else if (pool.size < maxPoolSize) {
                val exoPlayer = createExoPlayer()
                val platformPlayer = PlatformPlayer(exoPlayer)
                val pooledPlayer =
                    PooledExoPlayer(
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

    actual suspend fun releasePlayer(player: PlatformPlayer): Unit =
        mutex.withLock {
            pool.find { it.platformPlayer == player }?.let { pooledPlayer ->
                pooledPlayer.isInUse = false
                pooledPlayer.currentUrl = null
                pooledPlayer.platformPlayer.pause()
                pooledPlayer.platformPlayer.stop()
                pooledPlayer.platformPlayer.clearMediaItems()
                cleanup(pooledPlayer)
            }
        }

    private fun cleanup(pooledPlayer: PooledExoPlayer) {
        pooledPlayer.internalListener?.let { listener ->
            pooledPlayer.platformPlayer.removeListener(listener)
        }
    }

    @OptIn(UnstableApi::class)
    private fun setupPlayerForUrl(
        pooledPlayer: PooledExoPlayer,
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

        val videoUri = playerData.url.toUri()
        val mediaItem = MediaItem.fromUri(videoUri)
        val mediaSource =
            if (isHlsUrl(playerData.url)) {
                createHlsMediaSource(mediaItem)
            } else {
                createProgressiveMediaSource(mediaItem, context)
            }

        pooledPlayer.platformPlayer.setMediaSource(mediaSource)
        pooledPlayer.platformPlayer.seekTo(0)
        pooledPlayer.platformPlayer.prepare()
    }

    private fun setupInternalListener(pooledPlayer: PooledExoPlayer) {
        val listener =
            createPooledPlayerPerformanceListener(
                onStateChange = { playbackState ->
                    when (playbackState) {
                        Player.STATE_BUFFERING -> {
                            pooledPlayer.externalListener?.onBuffer()
                        }

                        Player.STATE_READY -> {
                            pooledPlayer.externalListener?.onReady()
                        }

                        Player.STATE_IDLE -> {
                            pooledPlayer.externalListener?.onIdle()
                        }

                        Player.STATE_ENDED -> {
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
        onStateChange: (Int) -> Unit,
        onError: (PlaybackException) -> Unit,
    ): Player.Listener =
        object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                onStateChange(playbackState)
            }

            override fun onPlayerError(error: PlaybackException) {
                error.printStackTrace()
                onError(error)
            }
        }

    @OptIn(UnstableApi::class)
    private fun createExoPlayer(): ExoPlayer {
        val renderersFactory =
            DefaultRenderersFactory(context)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
                .setEnableDecoderFallback(true)

        return ExoPlayer
            .Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context))
            .setRenderersFactory(renderersFactory)
            .build()
            .apply {
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                repeatMode = Player.REPEAT_MODE_OFF
                setHandleAudioBecomingNoisy(true)
                playWhenReady = false
                setForegroundMode(false)
            }
    }

    actual fun dispose() {
        pool.forEach { pooledPlayer ->
            cleanup(pooledPlayer)
            pooledPlayer.platformPlayer.release()
        }
        pool.clear()
    }

    actual fun onPlayBackStarted(playerData: PlayerData) {
        val pooledPlayer = pool.find { it.currentUrl == playerData.url }
        pooledPlayer?.externalListener?.onPlayBackStarted()
    }

    actual fun onPlayBackStopped(playerData: PlayerData) {
        val pooledPlayer = pool.find { it.currentUrl == playerData.url }
        pooledPlayer?.externalListener?.onPlayBackStopped()
    }
}
