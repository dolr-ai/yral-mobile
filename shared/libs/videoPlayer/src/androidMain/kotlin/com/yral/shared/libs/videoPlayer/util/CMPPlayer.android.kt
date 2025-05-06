@file:Suppress("MagicNumber")

package com.yral.shared.libs.videoPlayer.util

import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.yral.shared.libs.videoPlayer.model.PlayerSpeed
import com.yral.shared.libs.videoPlayer.model.ScreenResize
import com.yral.shared.libs.videoPlayer.rememberExoPlayerWithLifecycle
import com.yral.shared.libs.videoPlayer.rememberPlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(UnstableApi::class)
@Composable
actual fun CMPPlayer(
    modifier: Modifier,
    url: String,
    thumbnailUrl: String,
    prefetchThumbnails: List<String>,
    playerParams: CMPPlayerParams,
) {
    val context = LocalContext.current
    val exoPlayer = rememberExoPlayerWithLifecycle(url, context, playerParams.isPause)
    val playerView = rememberPlayerView(exoPlayer, context)

    var isBuffering by remember { mutableStateOf(false) }
    var showThumbnail by remember { mutableStateOf(true) }

    // Notify buffer state changes
    LaunchedEffect(isBuffering) {
        playerParams.bufferCallback(isBuffering)
    }

    // Update current time every second
    LaunchedEffect(exoPlayer) {
        while (isActive) {
            playerParams.currentTime(exoPlayer.currentPosition.coerceAtLeast(0L).toInt())
            delay(1000) // Delay for 1 second
        }
    }

    // Keep screen on while the player view is active
    LaunchedEffect(playerView) {
        playerView.keepScreenOn = true
    }

    LaunchedEffect(prefetchThumbnails) {
        prefetchThumbnails.forEach {
            prefetchThumbnail(
                context = context,
                url = it,
            )
        }
    }

    Box(modifier) {
        AndroidView(
            factory = { playerView },
            modifier = modifier,
            update = {
                exoPlayer.playWhenReady = !playerParams.isPause
                exoPlayer.volume = if (playerParams.isMute) 0f else 1f
                playerParams.sliderTime?.let { exoPlayer.seekTo(it.toLong()) }
                exoPlayer.setPlaybackSpeed(playerParams.speed.toFloat())
                playerView.artworkDisplayMode = PlayerView.ARTWORK_DISPLAY_MODE_FILL
                playerView.resizeMode =
                    when (playerParams.size) {
                        ScreenResize.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        ScreenResize.FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    }
            },
        )

        if (showThumbnail) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = "Thumbnail",
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black),
            )
        }

        // Manage player listener and lifecycle
        DisposableEffect(key1 = exoPlayer) {
            val listener =
                createPlayerListener(
                    playerParams.isSliding,
                    playerParams.totalTime,
                    playerParams.currentTime,
                    loadingState = { isBuffering = it },
                    playerParams.didEndVideo,
                    playerParams.loop,
                    exoPlayer,
                    hideThumbnail = {
                        if (showThumbnail) {
                            showThumbnail = false
                        }
                    },
                )

            exoPlayer.addListener(listener)

            onDispose {
                exoPlayer.removeListener(listener)
                exoPlayer.release()
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                exoPlayer.release()
                playerView.keepScreenOn = false
            }
        }
    }
}

private fun PlayerSpeed.toFloat(): Float =
    when (this) {
        PlayerSpeed.X0_5 -> 0.5f
        PlayerSpeed.X1 -> 1f
        PlayerSpeed.X1_5 -> 1.5f
        PlayerSpeed.X2 -> 2f
    }

private fun createPlayerListener(
    isSliding: Boolean,
    totalTime: (Int) -> Unit,
    currentTime: (Int) -> Unit,
    loadingState: (Boolean) -> Unit,
    didEndVideo: () -> Unit,
    loop: Boolean,
    exoPlayer: ExoPlayer,
    hideThumbnail: () -> Unit,
): Player.Listener =
    object : Player.Listener {
        override fun onEvents(
            player: Player,
            events: Player.Events,
        ) {
            if (!isSliding) {
                totalTime(player.duration.coerceAtLeast(0L).toInt())
                currentTime(player.currentPosition.coerceAtLeast(0L).toInt())
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    loadingState(true)
                }

                Player.STATE_READY -> {
                    loadingState(false)
                    hideThumbnail()
                }

                Player.STATE_ENDED -> {
                    loadingState(false)
                    didEndVideo()
                    exoPlayer.seekTo(0)
                    if (loop) exoPlayer.play()
                }

                Player.STATE_IDLE -> {
                    loadingState(false)
                }
            }
        }
    }

private fun prefetchThumbnail(
    context: Context,
    url: String,
) {
    val request =
        ImageRequest
            .Builder(context)
            .data(url)
            .build()

    val imageLoader = ImageLoader(context)
    imageLoader.enqueue(request)
}
