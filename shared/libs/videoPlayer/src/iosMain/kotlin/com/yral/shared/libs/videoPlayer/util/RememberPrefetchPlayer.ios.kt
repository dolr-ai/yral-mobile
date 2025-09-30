package com.yral.shared.libs.videoPlayer.util

import androidx.compose.runtime.Composable
import com.yral.shared.libs.videoPlayer.pool.PlatformPlayer

@Composable
actual fun rememberPrefetchPlayerWithLifecycle(): PlatformPlayer {
    return PlatformPlayer() // STUB
}

@Composable
actual fun PrefetchVideo(
    player: PlatformPlayer,
    url: String,
    listener: PrefetchVideoListener?,
    onUrlReady: (String) -> Unit,
) {
    // STUB
}
