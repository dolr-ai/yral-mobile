package com.yral.android.ui.screens.home.feed.performance

import com.yral.shared.libs.videoPlayer.model.Reels

interface IPrefetchPerformanceMonitor {
    var readyTrace: PrefetchReadyTrace?
    var loadTrace: PrefetchLoadTimeTrace?
    fun startTrace(
        type: PrefetchTraceType,
        reel: Reels,
    )
    fun stopTrace(type: PrefetchTraceType)
    fun stopTraceWithSuccess(type: PrefetchTraceType)
    fun stopTraceWithError(type: PrefetchTraceType)
}

enum class PrefetchTraceType {
    LOAD_TRACE,
    READY_TRACE,
}
