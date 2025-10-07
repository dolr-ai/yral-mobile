package com.yral.shared.libs.videoPlayer.model

data class PlayerControls(
    val isPause: Boolean,
    val isCurrentPage: Boolean = true,
    val onPauseToggle: (() -> Unit),
    val recordTime: (Int, Int) -> Unit,
)

data class PlayerInnerControls(
    val isMute: Boolean,
    val onMuteToggle: (() -> Unit),
    val onSpeedSelectionToggle: (() -> Unit),
    val onLockScreenToggle: (() -> Unit),
    val selectedSize: ScreenResize,
    val onResizeScreenToggle: (() -> Unit),
    val updateIsSliding: (Boolean) -> Unit,
    val onChangeSliderTime: ((Int?) -> Unit),
    val isFullScreen: Boolean,
    val onFullScreenToggle: (() -> Unit),
    val isLiveStream: Boolean,
    val showControls: Boolean,
    val currentTime: Int,
    val onChangeCurrentTime: ((Int) -> Unit),
    val totalTime: Int,
)
