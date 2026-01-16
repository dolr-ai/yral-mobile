package com.yral.shared.libs.videoplayback.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.media3.common.Player
import androidx.media3.ui.compose.ContentFrame
import com.yral.shared.libs.videoplayback.VideoSurfaceHandle
import java.util.UUID

internal class AndroidVideoSurfaceHandle(
    val playerState: MutableState<Player?>,
    override val id: String = UUID.randomUUID().toString(),
) : VideoSurfaceHandle

@Composable
actual fun VideoSurface(
    modifier: Modifier,
    shutter: @Composable () -> Unit,
    onHandleReady: (VideoSurfaceHandle) -> Unit,
) {
    val playerState = remember { mutableStateOf<Player?>(null) }
    val handle = remember { AndroidVideoSurfaceHandle(playerState) }

    ContentFrame(
        modifier = modifier,
        player = playerState.value,
        contentScale = ContentScale.Fit,
        shutter = shutter,
    )

    LaunchedEffect(handle) {
        onHandleReady(handle)
    }
}
