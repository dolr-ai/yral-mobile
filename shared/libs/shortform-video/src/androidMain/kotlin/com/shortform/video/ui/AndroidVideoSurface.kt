package com.shortform.video.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.shortform.video.VideoSurfaceHandle
import java.util.UUID

internal class AndroidVideoSurfaceHandle(
    val playerView: PlayerView,
    override val id: String = UUID.randomUUID().toString(),
) : VideoSurfaceHandle

@Composable
actual fun VideoSurface(
    modifier: Modifier,
    onHandleReady: (VideoSurfaceHandle) -> Unit,
) {
    val context = LocalContext.current
    val handle = remember {
        AndroidVideoSurfaceHandle(
            PlayerView(context).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            },
        )
    }

    AndroidView(
        factory = { handle.playerView },
        modifier = modifier,
    )

    LaunchedEffect(handle) {
        onHandleReady(handle)
    }
}
