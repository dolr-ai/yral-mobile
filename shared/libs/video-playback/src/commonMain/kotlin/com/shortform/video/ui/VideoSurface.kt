package com.shortform.video.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.shortform.video.VideoSurfaceHandle

@Composable
expect fun VideoSurface(
    modifier: Modifier = Modifier,
    shutter: @Composable () -> Unit = {},
    onHandleReady: (VideoSurfaceHandle) -> Unit,
)
