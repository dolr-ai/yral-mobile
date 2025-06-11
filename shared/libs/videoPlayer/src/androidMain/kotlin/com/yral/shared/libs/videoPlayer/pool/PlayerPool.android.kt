package com.yral.shared.libs.videoPlayer.pool

import android.content.Context
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.libs.videoPlayer.createHlsMediaSource
import com.yral.shared.libs.videoPlayer.createProgressiveMediaSource
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
        var performanceListener: Player.Listener? = null,
    )

    actual suspend fun getPlayer(url: String): PlatformPlayer =
        mutex.withLock {
            // Find available player or create new one
            val availablePlayer = pool.find { !it.isInUse }

            if (availablePlayer != null) {
                setupPlayerForUrl(availablePlayer, url)
                availablePlayer.platformPlayer
            } else if (pool.size < maxPoolSize) {
                val exoPlayer = createExoPlayer()
                val platformPlayer = PlatformPlayer(exoPlayer)
                val pooledPlayer = PooledExoPlayer(platformPlayer)
                pool.add(pooledPlayer)
                setupPlayerForUrl(pooledPlayer, url)
                platformPlayer
            } else {
                // Reclaim least recently used player (first in pool)
                val playerToReclaim = pool.removeAt(0)
                // Mark previous usage as released
                playerToReclaim.isInUse = false
                // Setup for new URL and mark as in use
                setupPlayerForUrl(playerToReclaim, url)
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
                // Stop playback and reset state
                pooledPlayer.platformPlayer.pause()
                pooledPlayer.platformPlayer.stop()
                pooledPlayer.platformPlayer.clearMediaItems()
            }
        }

    @OptIn(UnstableApi::class)
    private fun setupPlayerForUrl(
        pooledPlayer: PooledExoPlayer,
        url: String,
    ) {
        pooledPlayer.isInUse = true
        pooledPlayer.currentUrl = url

        // Reset player state completely
        pooledPlayer.platformPlayer.stop()
        pooledPlayer.platformPlayer.clearMediaItems()
        pooledPlayer.platformPlayer.pause() // Ensure consistent initial state

        val videoUri = url.toUri()
        val mediaItem = MediaItem.fromUri(videoUri)
        val mediaSource =
            if (isHlsUrl(url)) {
                createHlsMediaSource(mediaItem)
            } else {
                createProgressiveMediaSource(mediaItem, context)
            }

        pooledPlayer.platformPlayer.setMediaSource(mediaSource)
        pooledPlayer.platformPlayer.seekTo(0, 0)
        pooledPlayer.platformPlayer.prepare()
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
            pooledPlayer.platformPlayer.release()
        }
        pool.clear()
    }
}
