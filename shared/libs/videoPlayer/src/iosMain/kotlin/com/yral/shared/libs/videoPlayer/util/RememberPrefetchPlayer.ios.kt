package com.yral.shared.libs.videoPlayer.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.yral.shared.libs.videoPlayer.PlatformPlayer

@Composable
actual fun rememberPlatformPlayer(): PlatformPlayer {
    val platformPlayer = remember { PlatformPlayer() } // STUB
    DisposableEffect(key1 = platformPlayer) {
        onDispose {
            platformPlayer.release()
        }
    }
    return platformPlayer
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
