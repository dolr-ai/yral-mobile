package com.yral.shared.libs.videoPlayer.pool

import androidx.compose.runtime.Composable

@Composable
actual fun rememberPlayerPool(maxPoolSize: Int): PlayerPool {
    return PlayerPool(maxPoolSize) // STUB
}
