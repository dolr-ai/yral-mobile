package org.example.yarldemo.shared.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.example.yarldemo.shared.model.PlayerSpeed
import org.example.yarldemo.shared.model.ScreenResize

@Composable
actual fun CMPPlayer(
    modifier: Modifier,
    url: String,
    isPause: Boolean,
    isMute: Boolean,
    totalTime: (Int) -> Unit,
    currentTime: (Int) -> Unit,
    isSliding: Boolean,
    sliderTime: Int?,
    speed: PlayerSpeed,
    size: ScreenResize,
    bufferCallback: (Boolean) -> Unit,
    didEndVideo: () -> Unit,
    loop: Boolean,
    volume: Float,
)
