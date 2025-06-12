package com.yral.shared.libs.videoPlayer.util

import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.yral.shared.libs.videoPlayer.createHlsMediaSource
import com.yral.shared.libs.videoPlayer.createProgressiveMediaSource
import com.yral.shared.libs.videoPlayer.getExoPlayerLifecycleObserver
import com.yral.shared.libs.videoPlayer.performance.PrefetchDownloadTrace
import com.yral.shared.libs.videoPlayer.performance.PrefetchLoadTimeTrace
import com.yral.shared.libs.videoPlayer.performance.VideoPerformanceFactoryProvider
import com.yral.shared.libs.videoPlayer.pool.PlatformPlayer

@OptIn(UnstableApi::class)
@Composable
actual fun rememberPrefetchPlayerWithLifecycle(): PlatformPlayer {
    val context = LocalContext.current
    val exoPlayer = remember(context) { createExoPlayer(context) }
    DisposableEffect(key1 = exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
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
    return PlatformPlayer(exoPlayer)
}

@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
actual fun PrefetchVideo(
    player: PlatformPlayer,
    url: String,
    videoId: String,
    onUrlReady: () -> Unit,
) {
    if (url.isEmpty()) return
    var currentUrl by remember { mutableStateOf("") }
    var setupPlayer by remember { mutableStateOf(false) }
    // Performance monitoring traces for prefetch - download and load time
    var downloadTrace by remember { mutableStateOf<PrefetchDownloadTrace?>(null) }
    var loadTrace by remember { mutableStateOf<PrefetchLoadTimeTrace?>(null) }
    var currentPlayerState by remember { mutableIntStateOf(Player.STATE_IDLE) }
    LaunchedEffect(currentPlayerState) {
        when (currentPlayerState) {
            Player.STATE_BUFFERING -> {
                // Stop VideoStartup_prefetch trace when buffering starts (decoder initialized)
                loadTrace?.let { trace ->
                    trace.success()
                    loadTrace = null
                }
            }

            Player.STATE_READY -> {
                println("Logging trace metric: prefetch video ready")
                // Stop VideoDownload_prefetch trace when prefetch is ready (network data received)
                downloadTrace?.let { trace ->
                    trace.success()
                    downloadTrace = null
                }
                onUrlReady()
            }

            Player.STATE_IDLE -> {
                // STATE_IDLE can occur during normal cleanup or errors
                // Don't automatically treat as error - let onPlayerError handle actual errors
                // Just clean up traces without marking as error
                downloadTrace?.let { trace ->
                    trace.stop()
                    downloadTrace = null
                }
                loadTrace?.let { trace ->
                    trace.stop()
                    loadTrace = null
                }
            }
        }
    }
    LaunchedEffect(url) {
        if (currentUrl != url) {
            currentUrl = url
            setupPlayer = true
        }
    }
    if (setupPlayer) {
        setupPlayer = false
        currentPlayerState = Player.STATE_IDLE
        downloadTrace?.stop()
        loadTrace?.stop()
        // Start download and load traces for prefetch
        downloadTrace =
            VideoPerformanceFactoryProvider
                .createPrefetchDownloadTrace(url, videoId)
                .apply { start() }
        loadTrace =
            VideoPerformanceFactoryProvider
                .createPrefetchLoadTimeTrace(url, videoId)
                .apply { start() }

        val context = LocalContext.current
        player.stop()
        player.clearMediaItems()
        player.pause() // Ensure consistent initial state

        val videoUri = url.toUri()
        val mediaItem = MediaItem.fromUri(videoUri)
        val mediaSource =
            if (isHlsUrl(url)) {
                createHlsMediaSource(mediaItem)
            } else {
                createProgressiveMediaSource(mediaItem, context)
            }

        player.setMediaSource(mediaSource)
        player.seekTo(0, 0)
        player.prepare()
    }
    DisposableEffect(currentUrl) {
        val listener =
            createPrefetchPlayerListener(
                onStateChange = { currentPlayerState = it },
                onErr = {
                    // Stop all traces on prefetch error
                    downloadTrace?.let { trace ->
                        trace.error()
                        downloadTrace = null
                    }
                    loadTrace?.let { trace ->
                        trace.error()
                        loadTrace = null
                    }
                },
            )
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
        }
    }
}

/**
 * Creates a performance monitoring listener for prefetch video player.
 * Handles both VideoDownload_prefetch and VideoStartup_prefetch traces.
 */
private fun createPrefetchPlayerListener(
    onStateChange: (Int) -> Unit,
    onErr: (PlaybackException) -> Unit,
): Player.Listener =
    object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            onStateChange(playbackState)
        }

        override fun onPlayerError(error: PlaybackException) {
            onErr(error)
        }
    }

@OptIn(UnstableApi::class)
private fun createExoPlayer(context: Context): ExoPlayer {
    val renderersFactory =
        DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            .setEnableDecoderFallback(true)
    val loadControl =
        DefaultLoadControl
            .Builder()
            .setBufferDurationsMs(
                MIN_BUFFER_MS,
                MIN_BUFFER_MS,
                BUFFER_MS_FOR_PLAYBACK,
                BUFFER_MS_FOR_PLAYBACK,
            ).build()
    return ExoPlayer
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

private const val MIN_BUFFER_MS = 3000
private const val BUFFER_MS_FOR_PLAYBACK = 1000
