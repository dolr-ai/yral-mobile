package com.yral.shared.libs.videoPlayer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun YralVideoPlayer(
    modifier: Modifier = Modifier,
    url: String,
    autoPlay: Boolean = false,
    loop: Boolean = true,
    videoResizeMode: ResizeMode = ResizeMode.FIT,
    onError: (String) -> Unit = {},
)

enum class ResizeMode {
    FIT,
    FIXED_WIDTH,
}
