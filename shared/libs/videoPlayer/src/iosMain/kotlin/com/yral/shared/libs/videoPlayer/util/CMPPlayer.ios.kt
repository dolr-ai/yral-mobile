package com.yral.shared.libs.videoPlayer.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.yral.shared.libs.videoPlayer.PlatformPlayer
import com.yral.shared.libs.videoPlayer.PlatformPlayerError
import com.yral.shared.libs.videoPlayer.model.ScreenResize

internal actual fun isDecoderInitFailed(error: PlatformPlayerError): Boolean = false

@Composable
internal actual fun PlatformVideoPlayerView(
    modifier: Modifier,
    platformPlayer: PlatformPlayer?,
    screenResize: ScreenResize,
) {
    // STUB
}
