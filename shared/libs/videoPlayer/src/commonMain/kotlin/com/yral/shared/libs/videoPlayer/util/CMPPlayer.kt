package com.yral.shared.libs.videoPlayer.util

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
import co.touchlab.kermit.Logger
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.yral.shared.libs.videoPlayer.PlatformPlaybackState
import com.yral.shared.libs.videoPlayer.PlatformPlayer
import com.yral.shared.libs.videoPlayer.PlatformPlayerError
import com.yral.shared.libs.videoPlayer.PlatformPlayerListener
import com.yral.shared.libs.videoPlayer.model.PlayerData
import com.yral.shared.libs.videoPlayer.model.PlayerSpeed
import com.yral.shared.libs.videoPlayer.model.ScreenResize
import com.yral.shared.libs.videoPlayer.pool.PlayerPool
import com.yral.shared.libs.videoPlayer.pool.VideoListener
import com.yral.shared.libs.videoPlayer.rememberPooledPlatformPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.koin.compose.koinInject

@Suppress("LongMethod", "CyclomaticComplexMethod", "MagicNumber")
@Composable
fun CMPPlayer(
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
    val latestVideoListener = rememberUpdatedState(videoListener)

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
                        if (isDecoderInitFailed(error)) {
                            // Attempt to play again with a 1-second delay
                            currentPlayer.seekTo(0)
                            currentPlayer.prepare()

                            // For hardware decoder issues, we've already configured fallback in ExoPlayerHelper
                        } else {
                            isBuffering = false
                            latestVideoListener.value?.onPlayerError()
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

internal expect fun isDecoderInitFailed(error: PlatformPlayerError): Boolean

@Composable
internal expect fun PlatformVideoPlayerView(
    modifier: Modifier,
    platformPlayer: PlatformPlayer?,
    screenResize: ScreenResize,
)

data class CMPPlayerParams(
    val isPause: Boolean,
    val isMute: Boolean,
    val onTotalTimeChanged: ((Int) -> Unit),
    val onCurrentTimeChanged: ((Int) -> Unit),
    val isSliding: Boolean,
    val sliderTime: Int?,
    val speed: PlayerSpeed,
    val size: ScreenResize,
    val onBufferingChanged: ((Boolean) -> Unit),
    val onDidEndVideo: (() -> Unit),
    val loop: Boolean,
    val volume: Float,
)

@Composable
internal fun BindPlayerState(
    platformPlayer: PlatformPlayer?,
    playerParams: CMPPlayerParams,
) {
    LaunchedEffect(
        platformPlayer,
        playerParams.isPause,
        playerParams.isMute,
        playerParams.sliderTime,
        playerParams.speed,
    ) {
        if (platformPlayer == null) return@LaunchedEffect

        val shouldPlay = !playerParams.isPause
        if (shouldPlay) {
            platformPlayer.play()
        } else {
            platformPlayer.pause()
        }

        val desiredVolume = if (playerParams.isMute) 0f else 1f
        platformPlayer.setVolume(volume = desiredVolume)

        playerParams.sliderTime?.let { newPos ->
            platformPlayer.seekTo(newPos.toLong())
        }

        platformPlayer.setPlaybackSpeed(playerParams.speed.toFloat())
    }
}

@Suppress("MagicNumber")
private fun PlayerSpeed.toFloat(): Float =
    when (this) {
        PlayerSpeed.X0_5 -> 0.5f
        PlayerSpeed.X1 -> 1f
        PlayerSpeed.X1_5 -> 1.5f
        PlayerSpeed.X2 -> 2f
    }

@Composable
internal fun PrefetchThumbnail(
    url: String,
    imageLoader: ImageLoader = koinInject(),
) {
    Logger.d { "PrefetchThumbnail: $url" }
    val request =
        ImageRequest
            .Builder(LocalPlatformContext.current)
            .data(url)
            .build()
    imageLoader.enqueue(request)
}
