package com.yral.shared.libs.videoPlayer.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.yral.shared.libs.videoPlayer.model.PlayerData
import com.yral.shared.libs.videoPlayer.model.PlayerSpeed
import com.yral.shared.libs.videoPlayer.model.ScreenResize
import com.yral.shared.libs.videoPlayer.pool.PlayerPool

@Composable
expect fun CMPPlayer(
    modifier: Modifier,
    playerData: PlayerData,
    playerParams: CMPPlayerParams,
    playerPool: PlayerPool,
    isPlayerVisible: Boolean,
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
