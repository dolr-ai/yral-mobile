package com.yral.shared.libs.videoPlayer.pool

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberPlayerPool(
    maxPoolSize: Int,
    enablePerformanceTracing: Boolean,
): PlayerPool {
    val context = LocalContext.current
    return remember(context, maxPoolSize, enablePerformanceTracing) {
        PlayerPool(context, maxPoolSize, enablePerformanceTracing)
    }
}
