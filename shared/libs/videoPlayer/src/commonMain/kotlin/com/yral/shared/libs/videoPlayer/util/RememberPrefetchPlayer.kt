package com.yral.shared.libs.videoPlayer.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.yral.shared.libs.videoPlayer.PlatformPlayer
import com.yral.shared.libs.videoPlayer.getPlayerLifecycleObserver

@Composable
fun rememberPrefetchPlayerWithLifecycle(): PlatformPlayer {
    val platformPlayer = rememberPlatformPlayer()
    val lifecycleOwner = LocalLifecycleOwner.current
    var appInBackground by remember {
        mutableStateOf(false)
    }
    DisposableEffect(lifecycleOwner, appInBackground, platformPlayer) {
        val lifecycleObserver =
            getPlayerLifecycleObserver(platformPlayer, true, appInBackground) {
                appInBackground = it
            }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }
    return platformPlayer
}

@Composable
expect fun rememberPlatformPlayer(): PlatformPlayer

@Composable
expect fun PrefetchVideo(
    player: PlatformPlayer = rememberPrefetchPlayerWithLifecycle(),
    url: String,
    listener: PrefetchVideoListener?,
    onUrlReady: (url: String) -> Unit,
)

interface PrefetchVideoListener {
    fun onSetupPlayer()
    fun onBuffer()
    fun onReady()
    fun onIdle()
    fun onPlayerError()
}
