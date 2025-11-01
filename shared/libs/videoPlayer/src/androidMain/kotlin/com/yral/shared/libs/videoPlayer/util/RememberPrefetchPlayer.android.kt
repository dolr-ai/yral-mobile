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
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.yral.shared.libs.videoPlayer.PlatformPlaybackState
import com.yral.shared.libs.videoPlayer.PlatformPlayer
import com.yral.shared.libs.videoPlayer.PlatformPlayerError
import com.yral.shared.libs.videoPlayer.PlatformPlayerListener
import com.yral.shared.libs.videoPlayer.createHlsMediaSource
import com.yral.shared.libs.videoPlayer.createProgressiveMediaSource

@Composable
actual fun rememberPlatformPlayer(): PlatformPlayer {
    val context = LocalContext.current
    val platformPlayer = remember(context) { PlatformPlayer(createExoPlayer(context)) }
    DisposableEffect(key1 = platformPlayer) {
        onDispose {
            platformPlayer.release()
        }
    }
    return platformPlayer
}

@Composable
actual fun PrefetchVideo(
    player: PlatformPlayer,
    url: String,
    listener: PrefetchVideoListener?,
    onUrlReady: (url: String) -> Unit,
) {
    if (url.isEmpty()) return
    val context = LocalContext.current
    var currentPlayerState by remember { mutableStateOf(PlatformPlaybackState.IDLE) }
    LaunchedEffect(currentPlayerState) {
        when (currentPlayerState) {
            PlatformPlaybackState.BUFFERING -> {
                listener?.onBuffer()
            }

            PlatformPlaybackState.READY -> {
                listener?.onReady()
                onUrlReady(url)
            }

            PlatformPlaybackState.IDLE -> {
                listener?.onIdle()
            }

            PlatformPlaybackState.ENDED -> {
            }
        }
    }
    LaunchedEffect(url) {
        // Stop and reset the player *before* starting any new traces so that the
        // inevitable STATE_IDLE callback produced by `player.stop()` does not
        // clear traces that are about to be created in `onSetupPlayer()`.
        player.stop()
        player.clearMediaItems()
        player.pause() // Ensure consistent initial state

        // Now that the player is in a clean IDLE state, start the new traces and
        // set up the player for the incoming video.
        listener?.onSetupPlayer()

        val videoUri = url.toUri()
        val mediaItem = MediaItem.fromUri(videoUri)
        val mediaSource =
            if (isHlsUrl(url)) {
                createHlsMediaSource(mediaItem)
            } else {
                createProgressiveMediaSource(mediaItem, context)
            }

        player.setMediaSource(mediaSource)
        player.seekTo(0)
        player.prepare()
    }
    DisposableEffect(url) {
        val playerListener =
            createPrefetchPlayerListener(
                onStateChange = { currentPlayerState = it },
                onErr = { listener?.onPlayerError() },
            )
        player.addListener(playerListener)
        onDispose {
            player.removeListener(playerListener)
        }
    }
}

private fun createPrefetchPlayerListener(
    onStateChange: (PlatformPlaybackState) -> Unit,
    onErr: () -> Unit,
): PlatformPlayerListener =
    object : PlatformPlayerListener {
        override fun onPlaybackStateChanged(state: PlatformPlaybackState) {
            onStateChange(state)
        }

        override fun onPlayerError(error: PlatformPlayerError) {
            onErr()
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
