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
import com.yral.shared.libs.videoPlayer.performance.DownloadTrace
import com.yral.shared.libs.videoPlayer.performance.FirstFrameTrace
import com.yral.shared.libs.videoPlayer.performance.LoadTimeTrace
import com.yral.shared.libs.videoPlayer.performance.PlaybackTimeTrace
import com.yral.shared.libs.videoPlayer.performance.VideoPerformanceFactoryProvider
import com.yral.shared.libs.videoPlayer.util.isHlsUrl
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Android implementation of PlayerPool using ExoPlayer with integrated performance tracing
 */
actual class PlayerPool(
    private val context: Context,
    private val maxPoolSize: Int = 3,
    private val enablePerformanceTracing: Boolean = true,
) {
    actual constructor(maxPoolSize: Int) : this(
        throw YralException("Android PlayerPool requires context. Use rememberPlayerPool() instead."),
        maxPoolSize,
    )

    private val pool = mutableListOf<PooledExoPlayer>()
    private val mutex = Mutex()

    private data class PooledExoPlayer(
        val platformPlayer: PlatformPlayer,
        val exoPlayer: ExoPlayer,
        var isInUse: Boolean = false,
        var currentUrl: String? = null,
        var performanceListener: Player.Listener? = null,
        // Performance traces for current URL
        var downloadTrace: DownloadTrace? = null,
        var loadTrace: LoadTimeTrace? = null,
        var firstFrameTrace: FirstFrameTrace? = null,
        var playbackTimeTrace: PlaybackTimeTrace? = null,
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
                val pooledPlayer = PooledExoPlayer(platformPlayer, exoPlayer)
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
                // Clean up performance tracing
                cleanupPerformanceTracing(pooledPlayer)
                // Stop playback and reset state
                pooledPlayer.exoPlayer.playWhenReady = false
                pooledPlayer.exoPlayer.stop()
                pooledPlayer.exoPlayer.clearMediaItems()
            }
        }

    @OptIn(UnstableApi::class)
    private fun setupPlayerForUrl(
        pooledPlayer: PooledExoPlayer,
        url: String,
    ) {
        pooledPlayer.isInUse = true
        pooledPlayer.currentUrl = url

        val exoPlayer = pooledPlayer.exoPlayer

        // Clean up previous performance traces and listeners
        cleanupPerformanceTracing(pooledPlayer)

        // Reset player state completely
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        exoPlayer.playWhenReady = false // Ensure consistent initial state

        // Start performance tracing BEFORE setting up media source
        setupPerformanceTracing(pooledPlayer, url)

        val videoUri = url.toUri()
        val mediaItem = MediaItem.fromUri(videoUri)
        val mediaSource =
            if (isHlsUrl(url)) {
                createHlsMediaSource(mediaItem)
            } else {
                createProgressiveMediaSource(mediaItem, context)
            }

        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.seekTo(0, 0)
        exoPlayer.prepare()
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
            cleanupPerformanceTracing(pooledPlayer)
            pooledPlayer.exoPlayer.release()
        }
        pool.clear()
    }

    private fun setupPerformanceTracing(
        pooledPlayer: PooledExoPlayer,
        url: String,
    ) {
        if (!enablePerformanceTracing) return

        // Create performance traces
        pooledPlayer.downloadTrace =
            VideoPerformanceFactoryProvider
                .createDownloadTrace(url)
                .apply { start() }

        pooledPlayer.loadTrace =
            VideoPerformanceFactoryProvider
                .createLoadTimeTrace(url)
                .apply { start() }

        pooledPlayer.firstFrameTrace =
            VideoPerformanceFactoryProvider
                .createFirstFrameTrace(url)
                .apply { start() }

        pooledPlayer.playbackTimeTrace =
            VideoPerformanceFactoryProvider
                .createPlaybackTimeTrace(url)

        // Create and attach performance listener
        pooledPlayer.performanceListener = createPooledPlayerListener(pooledPlayer)
        pooledPlayer.exoPlayer.addListener(pooledPlayer.performanceListener!!)
    }

    private fun cleanupPerformanceTracing(pooledPlayer: PooledExoPlayer) {
        // Remove existing listener
        pooledPlayer.performanceListener?.let { listener ->
            pooledPlayer.exoPlayer.removeListener(listener)
            pooledPlayer.performanceListener = null
        }

        // Stop and cleanup traces
        pooledPlayer.downloadTrace?.stop()
        pooledPlayer.loadTrace?.stop()
        pooledPlayer.firstFrameTrace?.stop()
        pooledPlayer.playbackTimeTrace?.stop()

        pooledPlayer.downloadTrace = null
        pooledPlayer.loadTrace = null
        pooledPlayer.firstFrameTrace = null
        pooledPlayer.playbackTimeTrace = null
    }

    private fun createPooledPlayerListener(pooledPlayer: PooledExoPlayer): Player.Listener =
        object : Player.Listener {
            private var hasStartedPlayback = false
            private var hasCompletedFirstPlaythrough = false
            private var hasReachedReady = false
            private var hasRenderedFirstFrame = false

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        // Stop download trace when buffering starts (network data received)
                        pooledPlayer.downloadTrace?.let { trace ->
                            trace.success()
                            pooledPlayer.downloadTrace = null
                        }
                    }

                    Player.STATE_READY -> {
                        // Stop load trace when decoder is ready
                        pooledPlayer.loadTrace?.let { trace ->
                            trace.success()
                            pooledPlayer.loadTrace = null
                        }
                        hasReachedReady = true
                    }

                    Player.STATE_ENDED -> {
                        // Video completed - stop playback time trace if this is first completion
                        if (!hasCompletedFirstPlaythrough) {
                            pooledPlayer.playbackTimeTrace?.let { trace ->
                                trace.success()
                                pooledPlayer.playbackTimeTrace = null
                            }
                            hasCompletedFirstPlaythrough = true
                        }
                    }

                    Player.STATE_IDLE -> {
                        // Handle errors or idle state - mark traces as error
                        pooledPlayer.downloadTrace?.error()
                        pooledPlayer.loadTrace?.error()
                        pooledPlayer.firstFrameTrace?.error()
                        pooledPlayer.playbackTimeTrace?.error()

                        // Reset state flags
                        resetFlags()
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying && !hasStartedPlayback && !hasCompletedFirstPlaythrough) {
                    // Start playback time trace when video actually starts playing for the first time
                    pooledPlayer.playbackTimeTrace?.start()
                    hasStartedPlayback = true
                }
            }

            override fun onRenderedFirstFrame() {
                // Complete the trace on the FIRST onRenderedFirstFrame call only
                if (!hasRenderedFirstFrame) {
                    hasRenderedFirstFrame = true
                    pooledPlayer.firstFrameTrace?.let { trace ->
                        trace.success()
                        pooledPlayer.firstFrameTrace = null
                    }
                }
            }

            override fun onMediaItemTransition(
                mediaItem: MediaItem?,
                reason: Int,
            ) {
                // Reset playback state for new media items
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED ||
                    reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK
                ) {
                    resetFlags()
                }
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int,
            ) {
                // Handle seeks that might affect playback time measurement
                if (reason == Player.DISCONTINUITY_REASON_SEEK &&
                    !hasCompletedFirstPlaythrough &&
                    newPosition.positionMs < oldPosition.positionMs
                ) {
                    // User seeked backwards - this might be during first playthrough
                    pooledPlayer.playbackTimeTrace?.putAttribute("seek_during_playback", "true")
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                // Stop all traces on error
                pooledPlayer.downloadTrace?.error()
                pooledPlayer.loadTrace?.error()
                pooledPlayer.firstFrameTrace?.error()
                pooledPlayer.playbackTimeTrace?.error()

                resetFlags()
            }

            private fun resetFlags() {
                hasStartedPlayback = false
                hasCompletedFirstPlaythrough = false
                hasReachedReady = false
                hasRenderedFirstFrame = false
            }
        }
}

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

    // Internal access to ExoPlayer for Android-specific operations like performance monitoring
    internal val internalExoPlayer: ExoPlayer = exoPlayer
}
