package com.yral.shared.libs.videoPlayer.util

import androidx.compose.runtime.Composable
import com.yral.shared.libs.videoPlayer.pool.PlatformPlayer

@Composable
expect fun rememberPrefetchPlayerWithLifecycle(): PlatformPlayer

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

data class PrefetchVideoListenerCreator(
    val videoId: String,
    val url: String,
)
