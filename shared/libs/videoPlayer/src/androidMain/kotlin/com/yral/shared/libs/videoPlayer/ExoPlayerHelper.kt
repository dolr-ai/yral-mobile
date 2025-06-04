package com.yral.shared.libs.videoPlayer

import android.content.Context
import android.view.ViewGroup
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
import com.yral.shared.libs.videoPlayer.performance.PrefetchDownloadTrace
import com.yral.shared.libs.videoPlayer.performance.PrefetchLoadTimeTrace
import com.yral.shared.libs.videoPlayer.performance.VideoPerformanceFactoryProvider
import com.yral.shared.libs.videoPlayer.util.isHlsUrl

@androidx.annotation.OptIn(UnstableApi::class)
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

@Suppress("LongMethod")
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun rememberExoPlayerWithLifecycle(
    url: String,
    context: Context,
    isPause: Boolean,
): ExoPlayer {
    val lifecycleOwner = LocalLifecycleOwner.current

    // Performance monitoring traces
    var downloadTrace by remember { mutableStateOf<DownloadTrace?>(null) }
    var loadTrace by remember { mutableStateOf<LoadTimeTrace?>(null) }
    var firstFrameTrace by remember { mutableStateOf<FirstFrameTrace?>(null) }

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

    // Add performance monitoring listener
    DisposableEffect(exoPlayer) {
        val listener =
            createMainPlayerListener(
                downloadTrace = { downloadTrace },
                loadTrace = { loadTrace },
                firstFrameTrace = { firstFrameTrace },
                onDownloadTraceUpdate = { downloadTrace = it },
                onLoadTraceUpdate = { loadTrace = it },
                onFirstFrameTraceUpdate = { firstFrameTrace = it },
                url = url,
            )

        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            // Clean up any remaining traces
            downloadTrace?.stop()
            loadTrace?.stop()
            firstFrameTrace?.stop()
        }
    }

    LaunchedEffect(url) {
        if (url.isNotEmpty()) {
            // Start download trace
            downloadTrace =
                VideoPerformanceFactoryProvider
                    .createDownloadTrace(url)
                    .apply { start() }

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

            // Start load trace
            loadTrace =
                VideoPerformanceFactoryProvider
                    .createLoadTimeTrace(url)
                    .apply { start() }

            exoPlayer.setMediaSource(mediaSource)
            exoPlayer.prepare()

            // Don't stop download trace here - let the player listener handle it
            // when we know the data is actually loaded
        }
    }

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
@androidx.annotation.OptIn(UnstableApi::class)
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
): ExoPlayer? {
    if (url.isEmpty()) return null
    val lifecycleOwner = LocalLifecycleOwner.current

    // Performance monitoring traces for prefetch
    var downloadTrace by remember { mutableStateOf<PrefetchDownloadTrace?>(null) }
    var loadTrace by remember { mutableStateOf<PrefetchLoadTimeTrace?>(null) }

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

    // Add performance monitoring listener for prefetch
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

    LaunchedEffect(url) {
        // Start download trace for prefetch
        downloadTrace =
            VideoPerformanceFactoryProvider
                .createPrefetchDownloadTrace(url)
                .apply { start() }

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

        // Start load trace for prefetch
        loadTrace =
            VideoPerformanceFactoryProvider
                .createPrefetchLoadTimeTrace(url)
                .apply { start() }

        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()

        // Don't stop download trace here - let the player listener handle it
    }

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

@androidx.annotation.OptIn(UnstableApi::class)
private fun createHlsMediaSource(mediaItem: MediaItem): MediaSource {
    val dataSourceFactory =
        DefaultHttpDataSource
            .Factory()
            .setAllowCrossProtocolRedirects(true)
    return HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
}

@androidx.annotation.OptIn(UnstableApi::class)
private fun createProgressiveMediaSource(
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
 */
private fun createMainPlayerListener(
    downloadTrace: () -> DownloadTrace?,
    loadTrace: () -> LoadTimeTrace?,
    firstFrameTrace: () -> FirstFrameTrace?,
    onDownloadTraceUpdate: (DownloadTrace?) -> Unit,
    onLoadTraceUpdate: (LoadTimeTrace?) -> Unit,
    onFirstFrameTraceUpdate: (FirstFrameTrace?) -> Unit,
    url: String,
): Player.Listener =
    object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    // Start first frame trace when buffering starts
                    if (firstFrameTrace() == null) {
                        onFirstFrameTraceUpdate(
                            VideoPerformanceFactoryProvider
                                .createFirstFrameTrace(url)
                                .apply { start() },
                        )
                    }
                }

                Player.STATE_READY -> {
                    // Stop download trace when buffering is complete (data downloaded)
                    downloadTrace()?.success()
                    onDownloadTraceUpdate(null)

                    // Stop load trace when player is ready (decoder ready)
                    loadTrace()?.success()
                    onLoadTraceUpdate(null)

                    // Stop first frame trace when ready to play (first frame ready)
                    firstFrameTrace()?.success()
                    onFirstFrameTraceUpdate(null)
                }

                Player.STATE_ENDED -> {
                    // Clean up any remaining traces
                    firstFrameTrace()?.success()
                    onFirstFrameTraceUpdate(null)
                }

                Player.STATE_IDLE -> {
                    // Handle errors or idle state
                    downloadTrace()?.error()
                    onDownloadTraceUpdate(null)
                    loadTrace()?.error()
                    onLoadTraceUpdate(null)
                    firstFrameTrace()?.error()
                    onFirstFrameTraceUpdate(null)
                }
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            // Stop all traces on error
            downloadTrace()?.error()
            onDownloadTraceUpdate(null)
            loadTrace()?.error()
            onLoadTraceUpdate(null)
            firstFrameTrace()?.error()
            onFirstFrameTraceUpdate(null)
        }
    }

/**
 * Creates a performance monitoring listener for prefetch video player
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
                Player.STATE_READY -> {
                    // Stop download trace when prefetch buffering is complete
                    downloadTrace()?.success()
                    onDownloadTraceUpdate(null)

                    // Stop load trace when prefetch player is ready
                    loadTrace()?.success()
                    onLoadTraceUpdate(null)
                }

                Player.STATE_IDLE -> {
                    // Handle errors for prefetch
                    downloadTrace()?.error()
                    onDownloadTraceUpdate(null)
                    loadTrace()?.error()
                    onLoadTraceUpdate(null)
                }
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            // Stop traces on prefetch error
            downloadTrace()?.error()
            onDownloadTraceUpdate(null)
            loadTrace()?.error()
            onLoadTraceUpdate(null)
        }
    }
