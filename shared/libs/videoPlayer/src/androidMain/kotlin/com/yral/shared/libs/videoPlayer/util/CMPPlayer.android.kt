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
import androidx.compose.runtime.key
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
import com.yral.shared.libs.videoPlayer.model.PlayerData
import com.yral.shared.libs.videoPlayer.model.PlayerSpeed
import com.yral.shared.libs.videoPlayer.model.ScreenResize
import com.yral.shared.libs.videoPlayer.pool.PlayerPool
import com.yral.shared.libs.videoPlayer.pool.VideoListener
import com.yral.shared.libs.videoPlayer.rememberPlayerView
import com.yral.shared.libs.videoPlayer.rememberPooledExoPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Suppress("LongMethod", "CyclomaticComplexMethod")
@OptIn(UnstableApi::class)
@Composable
actual fun CMPPlayer(
    modifier: Modifier,
    playerData: PlayerData,
    playerParams: CMPPlayerParams,
    playerPool: PlayerPool,
    videoListener: VideoListener?,
) {
    val context = LocalContext.current
    val exoPlayer =
        rememberPooledExoPlayer(
            playerData = playerData,
            playerPool = playerPool,
            isPause = playerParams.isPause,
            videoListener = videoListener,
        )

    val playerView = exoPlayer?.let { rememberPlayerView(it, context) }

    var isBuffering by remember { mutableStateOf(false) }
    var showThumbnail by remember { mutableStateOf(true) }

    // Stop audio immediately when not current page (during scroll)
    LaunchedEffect(playerParams.isCurrentPage, exoPlayer) {
        if (!playerParams.isCurrentPage && exoPlayer != null) {
            exoPlayer.playWhenReady = false
            exoPlayer.volume = 0f
            exoPlayer.stop()
        }
    }

    // Disconnect player when paused (after scroll settles)
    LaunchedEffect(playerParams.isPause, exoPlayer, playerView) {
        if (playerParams.isPause) {
            exoPlayer?.let {
                it.playWhenReady = false
                it.volume = 0f
                it.stop()
            }
            playerView?.player = null
        } else if (exoPlayer != null && playerView != null) {
            playerView.player = exoPlayer
        }
    }

    // Notify buffer state changes
    LaunchedEffect(isBuffering) {
        playerParams.bufferCallback(isBuffering)
    }

    // Update current time every second
    LaunchedEffect(exoPlayer) {
        while (isActive && exoPlayer != null) {
            playerParams.currentTime(exoPlayer.currentPosition.coerceAtLeast(0L).toInt())
            delay(1000) // Delay for 1 second
        }
    }

    // Keep screen on while the player view is active
    LaunchedEffect(playerView) {
        playerView?.keepScreenOn = true
    }

    LaunchedEffect(playerData.prefetchThumbnails) {
        playerData.prefetchThumbnails.forEach {
            prefetchThumbnail(
                context = context,
                url = it,
            )
        }
    }

    Box(modifier) {
        if (exoPlayer != null && playerView != null && !playerParams.isPause) {
            key(playerData.url) {
                AndroidView(
                    factory = { playerView.apply { player = exoPlayer } },
                    modifier = modifier,
                    onRelease = { view ->
                        view.player?.let { p ->
                            if (p is ExoPlayer) {
                                p.playWhenReady = false
                                p.volume = 0f
                                p.stop()
                            }
                        }
                        view.player = null
                    },
                    update = { view ->
                        if (view.player == exoPlayer) {
                            exoPlayer.playWhenReady = !playerParams.isPause
                            exoPlayer.volume = if (playerParams.isMute) 0f else 1f
                            playerParams.sliderTime?.let { exoPlayer.seekTo(it.toLong()) }
                            exoPlayer.setPlaybackSpeed(playerParams.speed.toFloat())
                            view.artworkDisplayMode = PlayerView.ARTWORK_DISPLAY_MODE_FILL
                            view.resizeMode =
                                when (playerParams.size) {
                                    ScreenResize.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    ScreenResize.FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                }
                        }
                    },
                )
            }
        }
        if (showThumbnail) {
            AsyncImage(
                model = playerData.thumbnailUrl,
                contentDescription = "Thumbnail",
                contentScale = ContentScale.Fit,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black),
            )
        }
    }

    exoPlayer?.let {
        // Manage player listener and lifecycle
        DisposableEffect(exoPlayer) {
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
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            playerView?.keepScreenOn = false
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

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            // Check if it's a decoder initialization exception
            if (error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_DECODER_INIT_FAILED) {
                // Attempt to play again with a 1-second delay
                exoPlayer.seekTo(0)
                exoPlayer.prepare()

                // For hardware decoder issues, we've already configured fallback in ExoPlayerHelper
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
