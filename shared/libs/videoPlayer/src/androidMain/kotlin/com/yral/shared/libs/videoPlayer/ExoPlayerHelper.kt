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
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
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
    rememberMediaSetup(url, exoPlayer, context)

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

@OptIn(UnstableApi::class)
@Composable
private fun rememberMediaSetup(
    url: String,
    exoPlayer: ExoPlayer,
    context: Context,
) {
    // Consolidated LaunchedEffect for both media setup and tracing to ensure proper timing
    LaunchedEffect(url) {
        if (url.isNotEmpty()) {
            // set up media source after traces have been started
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
