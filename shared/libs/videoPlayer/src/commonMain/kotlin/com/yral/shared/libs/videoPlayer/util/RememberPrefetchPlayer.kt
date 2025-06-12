package com.yral.shared.libs.videoPlayer.util

import androidx.compose.runtime.Composable
import com.yral.shared.libs.videoPlayer.pool.PlatformPlayer

@Composable
expect fun rememberPrefetchPlayerWithLifecycle(): PlatformPlayer

@Composable
expect fun PrefetchVideo(
    player: PlatformPlayer = rememberPrefetchPlayerWithLifecycle(),
    url: String,
    videoId: String,
    performanceMonitor: PrefetchPerformanceMonitor? = null,
    onUrlReady: () -> Unit,
)

interface PrefetchPerformanceMonitor {
    fun startTrace(
        type: PrefetchTraceType,
        url: String,
        videoId: String,
    )
    fun stopTrace(type: PrefetchTraceType)
    fun stopTraceWithSuccess(type: PrefetchTraceType)
    fun stopTraceWithError(type: PrefetchTraceType)
}

enum class PrefetchTraceType {
    LOAD_TRACE,
    DOWNLOAD_TRACE,
}
