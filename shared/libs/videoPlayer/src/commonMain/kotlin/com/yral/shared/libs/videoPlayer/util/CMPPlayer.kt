package com.yral.shared.libs.videoPlayer.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.yral.shared.libs.videoPlayer.model.PlayerSpeed
import com.yral.shared.libs.videoPlayer.model.ScreenResize

@Composable
expect fun CMPPlayer(
    modifier: Modifier,
    url: String,
    isPause: Boolean,
    isMute: Boolean,
    totalTime: ((Int) -> Unit),
    currentTime: ((Int) -> Unit),
    isSliding: Boolean,
    sliderTime: Int?,
    speed: PlayerSpeed,
    size: ScreenResize,
    bufferCallback: ((Boolean) -> Unit),
    didEndVideo: (() -> Unit),
    loop: Boolean,
    volume: Float,
)
