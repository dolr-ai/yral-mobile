package com.shortform.video.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.ContentFrame
import com.shortform.video.VideoSurfaceHandle
import java.util.UUID

internal class AndroidVideoSurfaceHandle(
    val playerState: androidx.compose.runtime.MutableState<Player?>,
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
        contentScale = ContentScale.Crop,
        shutter = shutter,
    )

    LaunchedEffect(handle) {
        onHandleReady(handle)
    }
}
