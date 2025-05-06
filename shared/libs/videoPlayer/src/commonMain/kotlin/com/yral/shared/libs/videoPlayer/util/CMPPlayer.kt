package com.yral.shared.libs.videoPlayer.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.yral.shared.libs.videoPlayer.model.PlayerSpeed
import com.yral.shared.libs.videoPlayer.model.ScreenResize

@Composable
expect fun CMPPlayer(
    modifier: Modifier,
    url: String,
    thumbnailUrl: String,
    prefetchThumbnails: List<String>,
    playerParams: CMPPlayerParams,
)

data class CMPPlayerParams(
    val isPause: Boolean,
    val isMute: Boolean,
    val totalTime: ((Int) -> Unit),
    val currentTime: ((Int) -> Unit),
    val isSliding: Boolean,
    val sliderTime: Int?,
    val speed: PlayerSpeed,
    val size: ScreenResize,
    val bufferCallback: ((Boolean) -> Unit),
    val didEndVideo: (() -> Unit),
    val loop: Boolean,
    val volume: Float,
)
