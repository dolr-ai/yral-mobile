package com.yral.shared.libs.videoPlayer.util

import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.yral.shared.libs.videoPlayer.createHlsMediaSource
import com.yral.shared.libs.videoPlayer.createProgressiveMediaSource
import com.yral.shared.libs.videoPlayer.getExoPlayerLifecycleObserver
import com.yral.shared.libs.videoPlayer.pool.PlatformPlayer

@OptIn(UnstableApi::class)
@Composable
actual fun rememberPrefetchPlayerWithLifecycle(onUrlReady: () -> Unit): PlatformPlayer {
    val context = LocalContext.current
    val exoPlayer = remember(context) { createExoPlayer(context) }
    DisposableEffect(key1 = exoPlayer) {
        val listener =
            object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        onUrlReady()
                    }
                }
            }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
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

@Composable
actual fun PrefetchVideo(
    player: PlatformPlayer,
    url: String,
) {
    if (url.isEmpty()) return
    var currentUrl by remember { mutableStateOf("") }
    var setupPlayer by remember { mutableStateOf(false) }
    LaunchedEffect(url) {
        if (currentUrl != url) {
            currentUrl = url
            setupPlayer = true
        }
    }
    if (setupPlayer) {
        setupPlayer = false
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
