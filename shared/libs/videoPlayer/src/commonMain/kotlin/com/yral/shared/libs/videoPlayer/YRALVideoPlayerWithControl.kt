package com.yral.shared.libs.videoPlayer

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.yral.shared.libs.videoPlayer.model.PlayerConfig
import com.yral.shared.libs.videoPlayer.model.PlayerControls
import com.yral.shared.libs.videoPlayer.model.PlayerData
import com.yral.shared.libs.videoPlayer.model.PlayerInnerControls
import com.yral.shared.libs.videoPlayer.model.PlayerSpeed
import com.yral.shared.libs.videoPlayer.model.ScreenResize
import com.yral.shared.libs.videoPlayer.pool.PlayerPool
import com.yral.shared.libs.videoPlayer.pool.VideoListener
import com.yral.shared.libs.videoPlayer.pool.rememberPlayerPool
import com.yral.shared.libs.videoPlayer.ui.component.LoaderView
import com.yral.shared.libs.videoPlayer.ui.video.controls.ControlsView
import com.yral.shared.libs.videoPlayer.ui.video.controls.LockScreenView
import com.yral.shared.libs.videoPlayer.ui.video.controls.SpeedSelectionOverlay
import com.yral.shared.libs.videoPlayer.util.CMPPlayer
import com.yral.shared.libs.videoPlayer.util.CMPPlayerParams
import com.yral.shared.libs.videoPlayer.util.isLiveStream
import kotlinx.coroutines.delay

@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
internal fun YRALVideoPlayerWithControl(
    modifier: Modifier,
    playerData: PlayerData,
    playerConfig: PlayerConfig,
    playerControls: PlayerControls,
    playerPool: PlayerPool? = null, // Optional player pool for efficient resource management
    videoListener: VideoListener?,
) {
    lateinit var defaultPlayerPool: PlayerPool
    if (playerPool == null) {
        // Create multiplatform player pool for efficient resource management
        defaultPlayerPool = rememberPlayerPool(maxPoolSize = 3)

        // Clean up player pool when composable is disposed
        DisposableEffect(defaultPlayerPool) {
            onDispose {
                defaultPlayerPool.dispose()
            }
        }
    }

    var totalTime by remember { mutableIntStateOf(0) } // Total duration of the video
    var currentTime by remember { mutableIntStateOf(0) } // Current playback time
    var isSliding by remember { mutableStateOf(false) } // Flag indicating if the seek bar is being slid
    var sliderTime: Int? by remember { mutableStateOf(null) } // Time indicated by the seek bar
    var isMute by remember { mutableStateOf(false) } // Flag indicating if the audio is muted
    var selectedSpeed by remember { mutableStateOf(PlayerSpeed.X1) } // Selected playback speed
    var showSpeedSelection by remember { mutableStateOf(false) } // Selected playback speed
    var isScreenLocked by remember { mutableStateOf(false) }
    var screenSize by remember { mutableStateOf(playerConfig.defaultScreenResize) }
    var isBuffering by remember { mutableStateOf(true) }
    var isFullScreen by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) } // State for showing/hiding controls

    playerConfig.isMute?.let {
        isMute = it
    }

    LaunchedEffect(isBuffering) {
        playerConfig.bufferCallback?.invoke(isBuffering)
    }

    // Auto-hide controls if enabled
    if (playerConfig.isAutoHideControlEnabled) {
        LaunchedEffect(showControls) {
            if (showControls) {
                delay(timeMillis = (playerConfig.controlHideIntervalSeconds * 1000).toLong()) // Delay hiding controls
                if (isSliding.not()) {
                    showControls = false // Hide controls if seek bar is not being slid
                }
            }
        }
    }

    // Container for the video player and control components
    Box(
        modifier =
            modifier
                .pointerInput(Unit) {
                    detectTapGestures { _ ->
                        showControls = showControls.not() // Toggle show/hide controls on tap
                        showSpeedSelection = false
                    }
                },
    ) {
        // Video player component
        val onTotalTimeChanged = remember { { time: Int -> totalTime = time } }
        val onCurrentTimeChanged =
            remember {
                { time: Int ->
                    if (isSliding.not()) {
                        currentTime = time // Update current playback time
                        sliderTime = null // Reset slider time if not sliding
                    }
                }
            }
        val onBufferingChanged = remember { { buffering: Boolean -> isBuffering = buffering } }
        val onDidEndVideo =
            remember(playerConfig.loop, playerConfig.didEndVideo, playerControls.onPauseToggle) {
                {
                    playerConfig.didEndVideo?.invoke()
                    if (!playerConfig.loop) playerControls.onPauseToggle()
                }
            }
        CMPPlayer(
            modifier = modifier,
            playerData = playerData,
            playerPool = playerPool ?: defaultPlayerPool,
            videoListener = videoListener,
            playerParams =
                CMPPlayerParams(
                    isPause = playerControls.isPause,
                    isMute = isMute,
                    onTotalTimeChanged = onTotalTimeChanged, // Update total time of the video
                    onCurrentTimeChanged = onCurrentTimeChanged,
                    isSliding = isSliding, // Pass seek bar sliding state
                    sliderTime = sliderTime, // Pass seek bar slider time
                    speed = selectedSpeed, // Pass selected playback speed
                    size = screenSize,
                    onBufferingChanged = onBufferingChanged,
                    onDidEndVideo = onDidEndVideo,
                    loop = playerConfig.loop,
                    volume = if (isMute) 0f else 1f,
                ),
        )

        if (isScreenLocked.not()) {
            ControlsView(
                playerConfig = playerConfig,
                playerControls = playerControls,
                playerInnerControls =
                    PlayerInnerControls(
                        isMute = isMute,
                        onMuteToggle = {
                            playerConfig.muteCallback?.invoke(isMute.not())
                            isMute = isMute.not()
                        }, // Toggle mute/unMute
                        showControls = showControls,
                        onSpeedSelectionToggle = { showSpeedSelection = showSpeedSelection.not() },
                        isFullScreen = isFullScreen,
                        onFullScreenToggle = { isFullScreen = isFullScreen.not() },
                        onLockScreenToggle = { isScreenLocked = isScreenLocked.not() },
                        onResizeScreenToggle = {
                            screenSize =
                                when (screenSize) {
                                    ScreenResize.FIT -> ScreenResize.FILL
                                    ScreenResize.FILL -> ScreenResize.FIT
                                }
                        },
                        isLiveStream = isLiveStream(playerData.url),
                        selectedSize = screenSize,
                        updateIsSliding = { isSliding = it },
                        onChangeSliderTime = { sliderTime = it },
                        totalTime = totalTime,
                        currentTime = currentTime,
                        onChangeCurrentTime = { currentTime = it },
                    ),
            )
        } else if (playerConfig.isScreenLockEnabled) {
            LockScreenView(
                playerConfig = playerConfig,
                showControls = showControls,
                onLockScreenToggle = { isScreenLocked = isScreenLocked.not() },
            )
        }

        if (isBuffering) {
            LoaderView(playerConfig)
        }

        SpeedSelectionOverlay(
            playerConfig = playerConfig,
            selectedSpeed = selectedSpeed,
            selectedSpeedCallback = { selectedSpeed = it },
            showSpeedSelection = showSpeedSelection,
            showSpeedSelectionCallback = { showSpeedSelection = it },
        )
    }

    LaunchedEffect(currentTime) {
        if (currentTime > 0 && totalTime > 0) {
            playerControls.recordTime(
                currentTime,
                totalTime,
            )
        }
    }
}
