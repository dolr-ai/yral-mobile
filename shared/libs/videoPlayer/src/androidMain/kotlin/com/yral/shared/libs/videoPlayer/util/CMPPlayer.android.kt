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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.yral.shared.libs.videoPlayer.PlatformPlaybackState
import com.yral.shared.libs.videoPlayer.PlatformPlayer
import com.yral.shared.libs.videoPlayer.PlatformPlayerError
import com.yral.shared.libs.videoPlayer.PlatformPlayerListener
import com.yral.shared.libs.videoPlayer.model.PlayerData
import com.yral.shared.libs.videoPlayer.model.ScreenResize
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
    val latestParams = rememberUpdatedState(playerParams)

    // Notify buffer state changes
    LaunchedEffect(isBuffering) {
        latestParams.value.onBufferingChanged(isBuffering)
    }

    // Update current time every second
    LaunchedEffect(platformPlayer) {
        while (isActive && platformPlayer != null) {
            val params = latestParams.value
            if (!params.isSliding) {
                params.onCurrentTimeChanged(platformPlayer.currentPosition().coerceAtLeast(0L).toInt())
            }
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

    platformPlayer?.let { currentPlayer ->
        DisposableEffect(currentPlayer) {
            val listener =
                object : PlatformPlayerListener {
                    override fun onDurationChanged(durationMs: Long) {
                        val params = latestParams.value
                        if (!params.isSliding) {
                            params.onTotalTimeChanged(durationMs.coerceAtLeast(0L).toInt())
                            params.onCurrentTimeChanged(platformPlayer.currentPosition().coerceAtLeast(0L).toInt())
                        }
                    }

                    override fun onPlaybackStateChanged(state: PlatformPlaybackState) {
                        when (state) {
                            PlatformPlaybackState.BUFFERING -> {
                                isBuffering = true
                            }

                            PlatformPlaybackState.READY -> {
                                isBuffering = false
                                if (showThumbnail) {
                                    showThumbnail = false
                                }
                            }

                            PlatformPlaybackState.ENDED -> {
                                isBuffering = false
                                val params = latestParams.value
                                params.onDidEndVideo()
                                currentPlayer.seekTo(0)
                                if (params.loop) {
                                    currentPlayer.play()
                                }
                            }

                            PlatformPlaybackState.IDLE -> {
                                isBuffering = false
                            }
                        }
                    }

                    override fun onPlayerError(error: PlatformPlayerError) {
                        // Check if it's a decoder initialization exception
                        if (error.code == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED) {
                            // Attempt to play again with a 1-second delay
                            currentPlayer.seekTo(0)
                            currentPlayer.prepare()

                            // For hardware decoder issues, we've already configured fallback in ExoPlayerHelper
                        }
                    }
                }

            currentPlayer.addListener(listener)

            onDispose {
                currentPlayer.removeListener(listener)
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
