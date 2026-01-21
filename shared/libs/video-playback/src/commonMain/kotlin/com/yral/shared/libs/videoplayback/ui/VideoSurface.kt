package com.yral.shared.libs.videoplayback.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.yral.shared.libs.videoplayback.VideoSurfaceHandle

@Composable
expect fun VideoSurface(
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    surfaceType: VideoSurfaceType = VideoSurfaceType.SurfaceView,
    shutter: @Composable () -> Unit = {},
    onHandleReady: (VideoSurfaceHandle) -> Unit,
)
