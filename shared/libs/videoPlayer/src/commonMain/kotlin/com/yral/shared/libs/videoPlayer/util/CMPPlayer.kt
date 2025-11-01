package com.yral.shared.libs.videoPlayer.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import co.touchlab.kermit.Logger
import coil3.ImageLoader
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.yral.shared.libs.videoPlayer.PlatformPlayer
import com.yral.shared.libs.videoPlayer.model.PlayerData
import com.yral.shared.libs.videoPlayer.model.PlayerSpeed
import com.yral.shared.libs.videoPlayer.model.ScreenResize
import com.yral.shared.libs.videoPlayer.pool.PlayerPool
import com.yral.shared.libs.videoPlayer.pool.VideoListener
import org.koin.compose.koinInject

@Composable
expect fun CMPPlayer(
    modifier: Modifier,
    playerData: PlayerData,
    playerParams: CMPPlayerParams,
    playerPool: PlayerPool,
    videoListener: VideoListener?,
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
