package com.yral.shared.libs.videoPlayer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun YralVideoPlayer(
    modifier: Modifier,
    url: String,
    autoPlay: Boolean,
    loop: Boolean,
    videoResizeMode: ResizeMode,
    onError: (String) -> Unit,
) {
    // STUB
}
