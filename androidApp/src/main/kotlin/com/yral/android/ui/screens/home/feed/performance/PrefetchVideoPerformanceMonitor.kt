package com.yral.android.ui.screens.home.feed.performance

import com.yral.android.ui.screens.home.feed.PrefetchLoadTimeTrace
import com.yral.android.ui.screens.home.feed.PrefetchReadyTrace

interface IPrefetchPerformanceMonitor {
    var readyTrace: PrefetchReadyTrace?
    var loadTrace: PrefetchLoadTimeTrace?
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
    READY_TRACE,
}
