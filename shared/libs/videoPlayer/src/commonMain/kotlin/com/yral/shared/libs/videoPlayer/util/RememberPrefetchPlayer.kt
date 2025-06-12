package com.yral.shared.libs.videoPlayer.util

import androidx.compose.runtime.Composable
import com.yral.shared.libs.videoPlayer.pool.PlatformPlayer

@Composable
expect fun rememberPrefetchPlayerWithLifecycle(): PlatformPlayer

@Composable
expect fun PrefetchVideo(
    player: PlatformPlayer = rememberPrefetchPlayerWithLifecycle(),
    url: String,
    onUrlReady: () -> Unit,
)
