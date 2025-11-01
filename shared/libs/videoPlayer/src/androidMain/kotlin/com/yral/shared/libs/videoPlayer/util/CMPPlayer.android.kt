@file:Suppress("MagicNumber")

package com.yral.shared.libs.videoPlayer.util

import android.view.ViewGroup
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.yral.shared.libs.videoPlayer.model.PlayerData
import com.yral.shared.libs.videoPlayer.model.ScreenResize
import com.yral.shared.libs.videoPlayer.pool.PlatformPlayer
import com.yral.shared.libs.videoPlayer.pool.PlayerPool
import com.yral.shared.libs.videoPlayer.pool.VideoListener
import com.yral.shared.libs.videoPlayer.rememberPooledPlatformPlayer
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
    val platformPlayer =
        rememberPooledPlatformPlayer(
            playerData = playerData,
            playerPool = playerPool,
            isPause = playerParams.isPause,
            videoListener = videoListener,
        )

    BindPlayerState(platformPlayer, playerParams)

    var isBuffering by remember { mutableStateOf(false) }
    var showThumbnail by remember { mutableStateOf(true) }

    // Notify buffer state changes
    LaunchedEffect(isBuffering) {
        playerParams.onBufferingChanged(isBuffering)
    }

    // Update current time every second
    LaunchedEffect(platformPlayer) {
        while (isActive && platformPlayer != null) {
            playerParams.onCurrentTimeChanged(platformPlayer.currentPosition().coerceAtLeast(0L).toInt())
            delay(1000) // Delay for 1 second
        }
    }
    playerData.prefetchThumbnails.forEach {
        PrefetchThumbnail(url = it)
    }

    Box(modifier) {
        // YralBlurredThumbnail(playerData.thumbnailUrl)
        PlatformVideoPlayerView(modifier, platformPlayer, playerParams.size)
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

    platformPlayer?.internalExoPlayer?.let { player ->
        // Manage player listener and lifecycle
        DisposableEffect(player) {
            val listener =
                createPlayerListener(
                    playerParams.isSliding,
                    playerParams.onTotalTimeChanged,
                    playerParams.onCurrentTimeChanged,
                    loadingState = { isBuffering = it },
                    playerParams.onDidEndVideo,
                    playerParams.loop,
                    player,
                    hideThumbnail = {
                        if (showThumbnail) {
                            showThumbnail = false
                        }
                    },
                )

            player.addListener(listener)

            onDispose {
                player.removeListener(listener)
            }
        }
    }
}

@Composable
private fun PlatformVideoPlayerView(
    modifier: Modifier,
    platformPlayer: PlatformPlayer?,
    screenResize: ScreenResize,
) {
    AndroidView(
        factory = { context ->
            PlayerView(context)
                .apply {
                    layoutParams =
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    artworkDisplayMode = PlayerView.ARTWORK_DISPLAY_MODE_FILL
                }
        },
        modifier = modifier,
        update = { playerView ->
            playerView.player = platformPlayer?.internalExoPlayer
            playerView.keepScreenOn = true
            playerView.resizeMode =
                when (screenResize) {
                    ScreenResize.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    ScreenResize.FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
        },
        onReset = { playerView ->
            playerView.keepScreenOn = false
            playerView.player = null
        },
        onRelease = { playerView ->
            playerView.keepScreenOn = false
            playerView.player = null
        },
    )
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
