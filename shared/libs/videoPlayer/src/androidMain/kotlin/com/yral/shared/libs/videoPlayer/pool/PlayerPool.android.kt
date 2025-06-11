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
import com.yral.shared.libs.videoPlayer.performance.VideoPerformanceConstants.BUFFERING_COUNT
import com.yral.shared.libs.videoPlayer.performance.VideoPerformanceFactoryProvider
import com.yral.shared.libs.videoPlayer.util.isHlsUrl
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Android implementation of PlayerPool using ExoPlayer with integrated performance tracing.
 *
 * Performance Traces:
 * - VideoDownload: Measures network performance (setup → STATE_READY)
 * - VideoStartup: Measures decoder initialization (setup → STATE_BUFFERING)
 * - FirstFrame: Measures render performance (visibility → first frame rendered)
 * - VideoPlayback: Measures complete playback experience (playing → ended)
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
        var isInUse: Boolean = false,
        var currentUrl: String? = null,
        var performanceListener: Player.Listener? = null,
        // Performance traces for current URL
        var downloadTrace: DownloadTrace? = null, // VideoDownload: setup → STATE_READY
        var loadTrace: LoadTimeTrace? = null, // VideoStartup: setup → STATE_BUFFERING
        var firstFrameTrace: FirstFrameTrace? = null, // FirstFrame: visibility → rendered
        var playbackTimeTrace: PlaybackTimeTrace? = null, // VideoPlayback: playing → ended
        var isFirstFrameTraceStarted: Boolean = false, // Track if FirstFrame trace has been started
        var shouldResetFirstFrameFlag: Boolean = false, // Flag to reset hasRenderedFirstFrame in listener
    )

    actual suspend fun getPlayer(url: String): PlatformPlayer =
        mutex.withLock {
            // First, check if we already have a player for this URL (even if marked as not in use)
            val existingPlayer = pool.find { it.currentUrl == url }
            if (existingPlayer != null) {
                // Reuse the existing player for this URL
                if (!existingPlayer.isInUse) {
                    existingPlayer.isInUse = true
                    // Move to end of pool (most recently used)
                    pool.remove(existingPlayer)
                    pool.add(existingPlayer)
                }
                return@withLock existingPlayer.platformPlayer
            }

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
                // Clean up performance tracing
                cleanupPerformanceTracing(pooledPlayer)
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
        // If already set up for this URL, don't recreate traces
        if (pooledPlayer.currentUrl == url && pooledPlayer.isInUse) {
            return
        }

        pooledPlayer.isInUse = true
        pooledPlayer.currentUrl = url

        // Clean up previous performance traces and listeners
        cleanupPerformanceTracing(pooledPlayer)

        // Reset player state completely
        pooledPlayer.platformPlayer.stop()
        pooledPlayer.platformPlayer.clearMediaItems()
        pooledPlayer.platformPlayer.pause() // Ensure consistent initial state

        // Start performance tracing BEFORE setting up media source
        // VideoDownload and VideoStartup traces start here
        setupPerformanceTracing(pooledPlayer, url)

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
            cleanupPerformanceTracing(pooledPlayer)
            pooledPlayer.platformPlayer.release()
        }
        pool.clear()
    }

    private fun setupPerformanceTracing(
        pooledPlayer: PooledExoPlayer,
        url: String,
    ) {
        if (!enablePerformanceTracing) return

        // Create performance traces
        // VideoDownload trace: measures network performance from setup to STATE_READY
        pooledPlayer.downloadTrace =
            VideoPerformanceFactoryProvider
                .createDownloadTrace(url)
                .apply { start() }

        // VideoStartup trace: measures decoder initialization from setup to STATE_BUFFERING
        pooledPlayer.loadTrace =
            VideoPerformanceFactoryProvider
                .createLoadTimeTrace(url)
                .apply { start() }

        // FirstFrame trace: measures render time from visibility to first frame (started later)
        pooledPlayer.firstFrameTrace =
            VideoPerformanceFactoryProvider
                .createFirstFrameTrace(url)
        pooledPlayer.isFirstFrameTraceStarted = false

        // VideoPlayback trace: measures complete playback from playing to ended (started later)
        pooledPlayer.playbackTimeTrace =
            VideoPerformanceFactoryProvider
                .createPlaybackTimeTrace(url)

        // Create and attach performance listener
        pooledPlayer.performanceListener = createPooledPlayerListener(pooledPlayer)
        pooledPlayer.platformPlayer.addListener(pooledPlayer.performanceListener!!)
    }

    private fun cleanupPerformanceTracing(pooledPlayer: PooledExoPlayer) {
        // Remove existing listener
        pooledPlayer.performanceListener?.let { listener ->
            pooledPlayer.platformPlayer.removeListener(listener)
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
        pooledPlayer.isFirstFrameTraceStarted = false
    }

    private fun createPooledPlayerListener(pooledPlayer: PooledExoPlayer): Player.Listener =
        object : Player.Listener {
            private var hasStartedPlayback = false
            private var hasCompletedFirstPlaythrough = false
            private var hasRenderedFirstFrame = false

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        // Stop VideoStartup trace when buffering starts (decoder initialized)
                        pooledPlayer.loadTrace?.let { trace ->
                            trace.success()
                            pooledPlayer.loadTrace = null
                        }
                        // Increment buffering count for VideoPlayback trace
                        pooledPlayer
                            .playbackTimeTrace
                            ?.incrementMetric(BUFFERING_COUNT, 1)
                    }

                    Player.STATE_READY -> {
                        // Stop VideoDownload trace when player is ready (network data received)
                        pooledPlayer.downloadTrace?.let { trace ->
                            trace.success()
                            pooledPlayer.downloadTrace = null
                        }
                    }

                    Player.STATE_ENDED -> {
                        // Video completed - stop VideoPlayback trace if this is first completion
                        if (!hasCompletedFirstPlaythrough) {
                            pooledPlayer.playbackTimeTrace?.let { trace ->
                                trace.success()
                                pooledPlayer.playbackTimeTrace = null
                            }
                            hasCompletedFirstPlaythrough = true
                        }
                    }

                    Player.STATE_IDLE -> {
                        // STATE_IDLE can occur during normal cleanup (stop/release) or errors
                        // Don't automatically treat as error - let onPlayerError handle actual errors
                        // Just clean up traces without marking as error
                        pooledPlayer.downloadTrace?.stop()
                        pooledPlayer.loadTrace?.stop()
                        pooledPlayer.firstFrameTrace?.stop()
                        pooledPlayer.playbackTimeTrace?.stop()

                        // Reset state flags
                        resetFlags()
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying && !hasStartedPlayback && !hasCompletedFirstPlaythrough) {
                    // Start VideoPlayback trace when video actually starts playing for the first time
                    pooledPlayer.playbackTimeTrace?.start()
                    hasStartedPlayback = true
                }
            }

            override fun onRenderedFirstFrame() {
                // Check if we need to reset the hasRenderedFirstFrame flag
                if (pooledPlayer.shouldResetFirstFrameFlag) {
                    hasRenderedFirstFrame = false
                    pooledPlayer.shouldResetFirstFrameFlag = false
                }
                // Complete the FirstFrame trace on the FIRST onRenderedFirstFrame call only
                // and only if the trace has been started (i.e., video is visible)
                if (!hasRenderedFirstFrame && pooledPlayer.isFirstFrameTraceStarted) {
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
                // Handle seeks that might affect VideoPlayback trace measurement
                if (reason == Player.DISCONTINUITY_REASON_SEEK &&
                    !hasCompletedFirstPlaythrough &&
                    newPosition.positionMs < oldPosition.positionMs
                ) {
                    // User seeked backwards - mark this in VideoPlayback trace
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
                hasRenderedFirstFrame = false
            }
        }

    /**
     * Starts the FirstFrame trace for a specific URL when it becomes visible.
     * This trace measures time from visibility to first frame rendered.
     */
    @OptIn(UnstableApi::class)
    actual suspend fun startFirstFrameTraceForUrl(url: String): Unit =
        mutex.withLock {
            val foundPlayer = pool.find { it.currentUrl == url && it.isInUse }
            if (foundPlayer == null) {
                return@withLock
            }
            foundPlayer.let { pooledPlayer ->
                if (!pooledPlayer.isFirstFrameTraceStarted &&
                    pooledPlayer.firstFrameTrace != null &&
                    pooledPlayer.currentUrl == url
                ) {
                    pooledPlayer.firstFrameTrace?.start()
                    pooledPlayer.isFirstFrameTraceStarted = true
                    // Check if first frame was already rendered during prefetch
                    // If so, complete the FirstFrame trace immediately since the video is ready to display
                    val playBackState = pooledPlayer.platformPlayer.getPlayBackState()
                    val videoFormat = pooledPlayer.platformPlayer.getVideoFormat()
                    if (playBackState == Player.STATE_READY && videoFormat != null) {
                        // Video is ready and has video format - first frame is available
                        pooledPlayer.firstFrameTrace?.success()
                        pooledPlayer.firstFrameTrace = null
                    } else {
                        // Video not ready yet, use reset mechanism for future callback
                        resetListenerFlags(pooledPlayer)
                    }
                }
            }
        }

    /**
     * Reset listener flags so FirstFrame trace can complete even if onRenderedFirstFrame
     * was called during setup/prefetch before the user actually sees the video.
     * This ensures accurate FirstFrame timing from visibility to render.
     */
    private fun resetListenerFlags(pooledPlayer: PooledExoPlayer) {
        pooledPlayer.shouldResetFirstFrameFlag = true
    }
}
