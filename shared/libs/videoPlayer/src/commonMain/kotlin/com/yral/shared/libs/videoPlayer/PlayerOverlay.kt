package com.yral.shared.libs.videoPlayer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import yral_mobile.shared.libs.videoplayer.generated.resources.Res
import yral_mobile.shared.libs.videoplayer.generated.resources.pause
import yral_mobile.shared.libs.videoplayer.generated.resources.play

@Composable
internal fun PlayerOverlay(
    modifier: Modifier,
    hasError: Boolean,
    isLoading: Boolean,
    isPlaying: Boolean,
    togglePlayPause: () -> Unit,
) {
    Box(modifier.fillMaxSize()) {
        // Custom Play/Pause Control Overlay
        if (!hasError) {
            PlayPauseControl(isPlaying, isLoading, togglePlayPause)
        }

        // Error state - simplified without red background
        if (hasError) {
            PlayerError()
        }
    }
}

@Composable
private fun PlayerError() {
    Box(
        modifier =
            Modifier.Companion
                .fillMaxSize()
                .background(Color.Companion.Black.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Companion.Center,
    ) {
        Icon(
            painter = painterResource(Res.drawable.play),
            contentDescription = "Video Error - Tap to retry",
            tint = Color.Companion.White.copy(alpha = 0.8f),
            modifier = Modifier.Companion.size(48.dp),
        )
    }
}

@Composable
private fun PlayPauseControl(
    isPlaying: Boolean,
    isLoading: Boolean,
    togglePlayPause: () -> Unit,
) {
    var isPlayPauseVisible by remember { mutableStateOf(true) }
    LaunchedEffect(isPlayPauseVisible) {
        delay(PLAY_PAUSE_BUTTON_TIMEOUT)
        if (isPlayPauseVisible) {
            isPlayPauseVisible = false
        }
    }
    Box(
        modifier =
            Modifier.Companion
                .fillMaxSize()
                .clickable { isPlayPauseVisible = !isPlayPauseVisible },
        contentAlignment = Alignment.Companion.Center,
    ) {
        if (!isLoading && isPlayPauseVisible) {
            Image(
                painter =
                    painterResource(
                        if (isPlaying) Res.drawable.pause else Res.drawable.play,
                    ),
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier =
                    Modifier.Companion
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable { togglePlayPause() },
            )
        }
    }
}

private const val PLAY_PAUSE_BUTTON_TIMEOUT = 2000L
