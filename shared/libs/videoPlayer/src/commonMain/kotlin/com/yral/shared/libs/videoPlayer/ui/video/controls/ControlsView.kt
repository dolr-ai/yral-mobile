package com.yral.shared.libs.videoPlayer.ui.video.controls

import androidx.compose.runtime.Composable
import com.yral.shared.libs.videoPlayer.extension.formattedInterval
import com.yral.shared.libs.videoPlayer.model.PlayerConfig
import com.yral.shared.libs.videoPlayer.model.PlayerControls
import com.yral.shared.libs.videoPlayer.model.PlayerInnerControls
import com.yral.shared.libs.videoPlayer.ui.component.LiveStreamView

@Suppress("LongMethod")
@Composable
internal fun ControlsView(
    playerConfig: PlayerConfig,
    playerControls: PlayerControls,
    playerInnerControls: PlayerInnerControls,
) {
    // Top control view for playback speed and mute/unMute
    TopControlView(
        playerConfig = playerConfig,
        isMute = playerInnerControls.isMute,
        onMuteToggle = playerInnerControls.onMuteToggle, // Toggle mute/unMute
        showControls = playerInnerControls.showControls, // Pass show/hide controls state
        onTapSpeed = playerInnerControls.onSpeedSelectionToggle,
        isFullScreen = playerInnerControls.isFullScreen,
        onFullScreenToggle = playerInnerControls.onFullScreenToggle,
        onLockScreenToggle = playerInnerControls.onLockScreenToggle,
        onResizeScreenToggle = playerInnerControls.onResizeScreenToggle,
        isLiveStream = playerInnerControls.isLiveStream,
        selectedSize = playerInnerControls.selectedSize,
    )

    // Center control view for pause/resume and fast forward/backward actions
    CenterControlView(
        playerConfig = playerConfig,
        isPause = playerControls.isPause,
        onPauseToggle = playerControls.onPauseToggle,
        onBackwardToggle = {
            // Seek backward
            playerInnerControls.updateIsSliding(true)
            val newTime =
                playerInnerControls.currentTime - playerConfig.fastForwardBackwardIntervalSeconds.formattedInterval()
            playerInnerControls.onChangeSliderTime(
                if (newTime < 0) {
                    0
                } else {
                    newTime
                },
            )
            playerInnerControls.updateIsSliding(false)
        },
        onForwardToggle = {
            // Seek forward
            playerInnerControls.updateIsSliding(true)
            val newTime =
                playerInnerControls.currentTime + playerConfig.fastForwardBackwardIntervalSeconds.formattedInterval()
            playerInnerControls.onChangeSliderTime(
                if (newTime > playerInnerControls.totalTime) {
                    playerInnerControls.totalTime
                } else {
                    newTime
                },
            )
            playerInnerControls.updateIsSliding(false)
        },
        showControls = playerInnerControls.showControls,
        isLiveStream = playerInnerControls.isLiveStream,
    )

    if (playerInnerControls.isLiveStream) {
        LiveStreamView(playerConfig)
    } else {
        // Bottom control view for seek bar and time duration display
        BottomControlView(
            playerConfig = playerConfig,
            currentTime = playerInnerControls.currentTime, // Pass current playback time
            totalTime = playerInnerControls.totalTime, // Pass total duration of the video
            showControls = playerInnerControls.showControls, // Pass show/hide controls state
            onChangeSliderTime = playerInnerControls.onChangeSliderTime,
            onChangeCurrentTime = playerInnerControls.onChangeCurrentTime,
            onChangeSliding = {
                // Update seek bar sliding state
                playerInnerControls.updateIsSliding(it)
            },
        )
    }
}
