package com.yral.shared.libs.videoPlayer

import android.content.Context
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.yral.shared.libs.videoPlayer.performance.DownloadTrace
import com.yral.shared.libs.videoPlayer.performance.FirstFrameTrace
import com.yral.shared.libs.videoPlayer.performance.LoadTimeTrace
import com.yral.shared.libs.videoPlayer.performance.PlaybackTimeTrace
import com.yral.shared.libs.videoPlayer.performance.PrefetchDownloadTrace
import com.yral.shared.libs.videoPlayer.performance.PrefetchLoadTimeTrace
import com.yral.shared.libs.videoPlayer.performance.VideoPerformanceFactoryProvider
import com.yral.shared.libs.videoPlayer.pool.PlatformPlayer
import com.yral.shared.libs.videoPlayer.pool.PlayerPool
import com.yral.shared.libs.videoPlayer.util.isHlsUrl

@OptIn(UnstableApi::class)
@Composable
fun rememberPlayerView(
    exoPlayer: ExoPlayer,
    context: Context,
): PlayerView {
    val playerView =
        remember(context) {
            PlayerView(context)
                .apply {
                    layoutParams =
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    player = exoPlayer
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                }
        }
    DisposableEffect(playerView) {
        onDispose {
            playerView.player = null
        }
    }
    return playerView
}

@OptIn(UnstableApi::class)
@Composable
fun rememberExoPlayerWithLifecycle(
    url: String,
    context: Context,
    isPause: Boolean,
    enablePerformanceTracing: Boolean = true, // Allow disabling traces when not needed
): ExoPlayer {
    val lifecycleOwner = LocalLifecycleOwner.current
    val exoPlayer =
        remember(context) {
            val renderersFactory =
                DefaultRenderersFactory(context)
                    .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
                    .setEnableDecoderFallback(true)
            ExoPlayer
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

    // Performance tracing and media setup - consolidated to ensure proper timing
    rememberMediaSetup(url, exoPlayer, context, enablePerformanceTracing)

    var appInBackground by remember {
        mutableStateOf(false)
    }
    DisposableEffect(key1 = lifecycleOwner, appInBackground) {
        val lifecycleObserver =
            getExoPlayerLifecycleObserver(exoPlayer, isPause, appInBackground) {
                appInBackground = it
            }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }

    return exoPlayer
}

@Suppress("LongMethod")
@OptIn(UnstableApi::class)
@Composable
fun rememberPrefetchExoPlayerWithLifecycle(
    url: String,
    context: Context,
    loadControl: LoadControl =
        DefaultLoadControl
            .Builder()
            .setBufferDurationsMs(
                MIN_BUFFER_MS,
                MIN_BUFFER_MS,
                BUFFER_MS_FOR_PLAYBACK,
                BUFFER_MS_FOR_PLAYBACK,
            ).build(),
    enablePerformanceTracing: Boolean = true,
): ExoPlayer? {
    if (url.isEmpty()) return null
    val lifecycleOwner = LocalLifecycleOwner.current
    val exoPlayer =
        remember(context) {
            val renderersFactory =
                DefaultRenderersFactory(context)
                    .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
                    .setEnableDecoderFallback(true)
            ExoPlayer
                .Builder(context)
                .setLoadControl(loadControl)
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

    // Consolidated prefetch performance tracing and media setup
    rememberPrefetchMediaSetup(url, exoPlayer, context, enablePerformanceTracing)

    var appInBackground by remember {
        mutableStateOf(false)
    }
    DisposableEffect(key1 = lifecycleOwner, appInBackground) {
        val lifecycleObserver =
            getExoPlayerLifecycleObserver(exoPlayer, true, appInBackground) {
                appInBackground = it
            }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }

    return exoPlayer
}

@Suppress("LongMethod")
@OptIn(UnstableApi::class)
@Composable
private fun rememberMediaSetup(
    url: String,
    exoPlayer: ExoPlayer,
    context: Context,
    enablePerformanceTracing: Boolean,
) {
    // Performance monitoring traces
    var downloadTrace by remember { mutableStateOf<DownloadTrace?>(null) }
    var loadTrace by remember { mutableStateOf<LoadTimeTrace?>(null) }
    var firstFrameTrace by remember { mutableStateOf<FirstFrameTrace?>(null) }
    var playbackTimeTrace by remember { mutableStateOf<PlaybackTimeTrace?>(null) }

    // State reset mechanism for new URLs
    var resetFlags by remember { mutableStateOf(false) }

    // Add performance monitoring listener - recreate for each player instance
    DisposableEffect(exoPlayer, url) {
        val listener =
            createMainPlayerListener(
                downloadTrace = { downloadTrace },
                loadTrace = { loadTrace },
                firstFrameTrace = { firstFrameTrace },
                playbackTimeTrace = { playbackTimeTrace },
                onDownloadTraceUpdate = { downloadTrace = it },
                onLoadTraceUpdate = { loadTrace = it },
                onFirstFrameTraceUpdate = { firstFrameTrace = it },
                onPlaybackTimeTraceUpdate = { playbackTimeTrace = it },
                resetFlags = { resetFlags },
                onResetFlagsHandled = { resetFlags = false },
            )

        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            // Clean up any remaining traces
            downloadTrace?.stop()
            loadTrace?.stop()
            firstFrameTrace?.stop()
            playbackTimeTrace?.stop()
        }
    }

    // Consolidated LaunchedEffect for both tracing and media setup to ensure proper timing
    LaunchedEffect(url) {
        if (url.isNotEmpty()) {
            // Clean up any existing traces before creating new ones
            downloadTrace?.stop()
            loadTrace?.stop()
            firstFrameTrace?.stop()
            playbackTimeTrace?.stop()

            // Create performance traces FIRST if enabled
            if (enablePerformanceTracing) {
                // Start download trace - measures network/media source loading time
                downloadTrace =
                    VideoPerformanceFactoryProvider
                        .createDownloadTrace(url)
                        .apply { start() }

                // Start load trace - measures decoder initialization time
                loadTrace =
                    VideoPerformanceFactoryProvider
                        .createLoadTimeTrace(url)
                        .apply { start() }

                // Create and start first frame trace - will be completed when first frame is rendered
                firstFrameTrace =
                    VideoPerformanceFactoryProvider
                        .createFirstFrameTrace(url)
                        .apply { start() }

                // Create playback time trace - will be started when actual playback begins
                playbackTimeTrace =
                    VideoPerformanceFactoryProvider
                        .createPlaybackTimeTrace(url)
            } else {
                downloadTrace = null
                loadTrace = null
                firstFrameTrace = null
                playbackTimeTrace = null
            }

            // Reset listener state flags for new URL
            resetFlags = true

            // NOW set up media source after traces have been started
            val videoUri = url.toUri()
            val mediaItem = MediaItem.fromUri(videoUri)
            // Reset the player to the start
            exoPlayer.seekTo(0, 0)
            // Prepare the appropriate media source based on the URL type
            val mediaSource =
                if (isHlsUrl(url)) {
                    createHlsMediaSource(mediaItem)
                } else {
                    createProgressiveMediaSource(mediaItem, context)
                }

            exoPlayer.setMediaSource(mediaSource)
            exoPlayer.prepare()
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun rememberPrefetchMediaSetup(
    url: String,
    exoPlayer: ExoPlayer,
    context: Context,
    enablePerformanceTracing: Boolean,
) {
    // Performance monitoring traces for prefetch - download and load time
    var downloadTrace by remember { mutableStateOf<PrefetchDownloadTrace?>(null) }
    var loadTrace by remember { mutableStateOf<PrefetchLoadTimeTrace?>(null) }

    if (enablePerformanceTracing) {
        // Add performance monitoring listener for prefetch - download and load traces
        DisposableEffect(exoPlayer) {
            val listener =
                createPrefetchPlayerListener(
                    downloadTrace = { downloadTrace },
                    loadTrace = { loadTrace },
                    onDownloadTraceUpdate = { downloadTrace = it },
                    onLoadTraceUpdate = { loadTrace = it },
                )

            exoPlayer.addListener(listener)

            onDispose {
                exoPlayer.removeListener(listener)
                // Clean up prefetch traces
                downloadTrace?.stop()
                loadTrace?.stop()
            }
        }
    }

    // Consolidated LaunchedEffect for prefetch tracing and media setup
    LaunchedEffect(url) {
        if (enablePerformanceTracing) {
            // Clean up any existing traces before creating new ones
            downloadTrace?.stop()
            loadTrace?.stop()

            // Start download and load traces for prefetch
            downloadTrace =
                VideoPerformanceFactoryProvider
                    .createPrefetchDownloadTrace(url)
                    .apply { start() }
            loadTrace =
                VideoPerformanceFactoryProvider
                    .createPrefetchLoadTimeTrace(url)
                    .apply { start() }
        }

        // NOW set up media source after traces have been started
        val videoUri = url.toUri()
        val mediaItem = MediaItem.fromUri(videoUri)
        // Reset the player to the start
        exoPlayer.seekTo(0, 0)
        // Prepare the appropriate media source based on the URL type
        val mediaSource =
            if (isHlsUrl(url)) {
                createHlsMediaSource(mediaItem)
            } else {
                createProgressiveMediaSource(mediaItem, context)
            }
        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
    }
}

@OptIn(UnstableApi::class)
@Suppress("LongMethod")
@Composable
fun rememberPooledExoPlayer(
    url: String,
    playerPool: PlayerPool,
    isPause: Boolean,
): ExoPlayer? {
    var platformPlayer: PlatformPlayer? by remember { mutableStateOf(null) }
    var exoPlayer: ExoPlayer? by remember { mutableStateOf(null) }

    // Get player from pool when URL changes
    LaunchedEffect(url, playerPool) {
        if (url.isNotEmpty()) {
            platformPlayer = playerPool.getPlayer(url)
            exoPlayer = platformPlayer?.internalExoPlayer
        }
    }

    // Note: Performance tracing is now handled internally by the PlayerPool
    // No need for additional tracing here to avoid duplicates

    val lifecycleOwner = LocalLifecycleOwner.current
    var appInBackground by remember {
        mutableStateOf(false)
    }
    exoPlayer?.let {
        DisposableEffect(key1 = lifecycleOwner, appInBackground) {
            val lifecycleObserver =
                getExoPlayerLifecycleObserver(it, isPause, appInBackground) {
                    appInBackground = it
                }
            lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            }
        }
    }

    return exoPlayer
}

@OptIn(UnstableApi::class)
internal fun createHlsMediaSource(mediaItem: MediaItem): MediaSource {
    val dataSourceFactory =
        DefaultHttpDataSource
            .Factory()
            .setAllowCrossProtocolRedirects(true)
    return HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
}

@OptIn(UnstableApi::class)
internal fun createProgressiveMediaSource(
    mediaItem: MediaItem,
    context: Context,
): MediaSource =
    ProgressiveMediaSource
        .Factory(
            MediaCache
                .getInstance(context)
                .cacheFactory,
        ).createMediaSource(mediaItem)

private const val MIN_BUFFER_MS = 5000
private const val BUFFER_MS_FOR_PLAYBACK = 1000

/**
 * Creates a performance monitoring listener for the main video player
 * Uses multiple ExoPlayer.Listener callbacks for comprehensive monitoring
 */
private fun createMainPlayerListener(
    downloadTrace: () -> DownloadTrace?,
    loadTrace: () -> LoadTimeTrace?,
    firstFrameTrace: () -> FirstFrameTrace?,
    playbackTimeTrace: () -> PlaybackTimeTrace?,
    onDownloadTraceUpdate: (DownloadTrace?) -> Unit,
    onLoadTraceUpdate: (LoadTimeTrace?) -> Unit,
    onFirstFrameTraceUpdate: (FirstFrameTrace?) -> Unit,
    onPlaybackTimeTraceUpdate: (PlaybackTimeTrace?) -> Unit,
    resetFlags: () -> Boolean,
    onResetFlagsHandled: () -> Unit,
): Player.Listener =
    object : Player.Listener {
        private var hasStartedPlayback = false
        private var hasCompletedFirstPlaythrough = false
        private var hasReachedReady = false
        private var hasRenderedFirstFrame = false

        override fun onPlaybackStateChanged(playbackState: Int) {
            // Check if we need to reset flags for new URL
            if (resetFlags()) {
                hasStartedPlayback = false
                hasCompletedFirstPlaythrough = false
                hasReachedReady = false
                hasRenderedFirstFrame = false
                onResetFlagsHandled()
            }

            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    // Stop download trace when buffering starts (network data received)
                    downloadTrace()?.let { trace ->
                        trace.success()
                        onDownloadTraceUpdate(null)
                    }
                    // First frame trace already started in LaunchedEffect
                    // Don't create new traces here to avoid duplicates
                }

                Player.STATE_READY -> {
                    // Stop load trace when decoder is ready
                    loadTrace()?.let { trace ->
                        trace.success()
                        onLoadTraceUpdate(null)
                    }

                    // Mark that we've reached ready state
                    hasReachedReady = true

                    // Don't complete FirstFrame trace here - let onRenderedFirstFrame handle it
                    // This prevents duplicate trace completions
                }

                Player.STATE_ENDED -> {
                    // Video completed - stop playback time trace if this is first completion
                    if (!hasCompletedFirstPlaythrough) {
                        playbackTimeTrace()?.let { trace ->
                            trace.success()
                            onPlaybackTimeTraceUpdate(null)
                        }
                        hasCompletedFirstPlaythrough = true
                    }
                }

                Player.STATE_IDLE -> {
                    // Handle errors or idle state
                    downloadTrace()?.let { trace ->
                        trace.error()
                        onDownloadTraceUpdate(null)
                    }
                    loadTrace()?.let { trace ->
                        trace.error()
                        onLoadTraceUpdate(null)
                    }
                    firstFrameTrace()?.let { trace ->
                        trace.error()
                        onFirstFrameTraceUpdate(null)
                    }
                    playbackTimeTrace()?.let { trace ->
                        trace.error()
                        onPlaybackTimeTraceUpdate(null)
                    }

                    // Reset state flags
                    hasStartedPlayback = false
                    hasCompletedFirstPlaythrough = false
                    hasReachedReady = false
                    hasRenderedFirstFrame = false
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying && !hasStartedPlayback && !hasCompletedFirstPlaythrough) {
                // Start playback time trace when video actually starts playing for the first time
                playbackTimeTrace()?.let { trace ->
                    trace.start()
                    hasStartedPlayback = true
                }
            }
        }

        override fun onRenderedFirstFrame() {
            // Complete the trace on the FIRST onRenderedFirstFrame call only
            if (!hasRenderedFirstFrame) {
                hasRenderedFirstFrame = true
                firstFrameTrace()?.let { trace ->
                    trace.success()
                    onFirstFrameTraceUpdate(null)
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
                hasStartedPlayback = false
                hasCompletedFirstPlaythrough = false
                hasReachedReady = false
                hasRenderedFirstFrame = false
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
                // Keep playback trace running but note the seek
                playbackTimeTrace()?.putAttribute("seek_during_playback", "true")
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            // Stop all traces on error
            downloadTrace()?.let { trace ->
                trace.error()
                onDownloadTraceUpdate(null)
            }
            loadTrace()?.let { trace ->
                trace.error()
                onLoadTraceUpdate(null)
            }
            firstFrameTrace()?.let { trace ->
                trace.error()
                onFirstFrameTraceUpdate(null)
            }
            playbackTimeTrace()?.let { trace ->
                trace.error()
                onPlaybackTimeTraceUpdate(null)
            }

            // Reset state flags
            hasStartedPlayback = false
            hasCompletedFirstPlaythrough = false
            hasReachedReady = false
            hasRenderedFirstFrame = false
        }
    }

/**
 * Creates a performance monitoring listener for prefetch video player.
 * Handles both VideoDownload_prefetch and VideoStartup_prefetch traces.
 */
private fun createPrefetchPlayerListener(
    downloadTrace: () -> PrefetchDownloadTrace?,
    loadTrace: () -> PrefetchLoadTimeTrace?,
    onDownloadTraceUpdate: (PrefetchDownloadTrace?) -> Unit,
    onLoadTraceUpdate: (PrefetchLoadTimeTrace?) -> Unit,
): Player.Listener =
    object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    // Stop VideoStartup_prefetch trace when buffering starts (decoder initialized)
                    loadTrace()?.let { trace ->
                        trace.success()
                        onLoadTraceUpdate(null)
                    }
                }

                Player.STATE_READY -> {
                    // Stop VideoDownload_prefetch trace when prefetch is ready (network data received)
                    downloadTrace()?.let { trace ->
                        trace.success()
                        onDownloadTraceUpdate(null)
                    }
                }

                Player.STATE_IDLE -> {
                    // STATE_IDLE can occur during normal cleanup or errors
                    // Don't automatically treat as error - let onPlayerError handle actual errors
                    // Just clean up traces without marking as error
                    downloadTrace()?.let { trace ->
                        trace.stop()
                        onDownloadTraceUpdate(null)
                    }
                    loadTrace()?.let { trace ->
                        trace.stop()
                        onLoadTraceUpdate(null)
                    }
                }
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            // Stop all traces on prefetch error
            downloadTrace()?.let { trace ->
                trace.error()
                onDownloadTraceUpdate(null)
            }
            loadTrace()?.let { trace ->
                trace.error()
                onLoadTraceUpdate(null)
            }
        }
    }
