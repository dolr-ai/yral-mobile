package com.yral.shared.libs.videoPlayer.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.yral.shared.libs.videoPlayer.PlatformPlayer
import com.yral.shared.libs.videoPlayer.prefetch.IosVideoPrefetchRegistry

@Composable
actual fun rememberPlatformPlayer(): PlatformPlayer {
    val platformPlayer = remember { PlatformPlayer() }
    DisposableEffect(key1 = platformPlayer) {
        onDispose {
            platformPlayer.release()
        }
    }
    return platformPlayer
}

@Composable
@Suppress("UNUSED_PARAMETER")
actual fun PrefetchVideo(
    player: PlatformPlayer,
    url: String,
    listener: PrefetchVideoListener?,
    onUrlReady: (String) -> Unit,
) {
    if (url.isEmpty()) return

    val listenerState = rememberUpdatedState(listener)
    val onReadyState = rememberUpdatedState(onUrlReady)

    DisposableEffect(url, listenerState.value, onReadyState.value) {
        listenerState.value?.onSetupPlayer()
        val handle =
            IosVideoPrefetchRegistry.register(
                url = url,
                listener = listenerState.value,
                onUrlReady = onReadyState.value,
            )
        onDispose {
            handle.dispose()
            listenerState.value?.onIdle()
        }
    }
}

actual fun evictPrefetchedVideo(url: String) {
    if (url.isEmpty()) return
    IosVideoPrefetchRegistry.evict(url)
}
