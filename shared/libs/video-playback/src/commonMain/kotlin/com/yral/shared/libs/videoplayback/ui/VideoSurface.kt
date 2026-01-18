package com.yral.shared.libs.videoplayback.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.yral.shared.libs.videoplayback.VideoSurfaceHandle

@Composable
expect fun VideoSurface(
    modifier: Modifier = Modifier,
    shutter: @Composable () -> Unit = {},
    onHandleReady: (VideoSurfaceHandle) -> Unit,
)
