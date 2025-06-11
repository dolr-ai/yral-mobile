package com.yral.android.ui.screens.home.feed.performance

interface PerformanceMonitor {
    var loadTrace: LoadTimeTrace?
    var firstFrameTrace: FirstFrameTrace?
    var playbackTimeTrace: PlaybackTimeTrace?

    fun startTrace(type: TraceType)
    fun stopTrace(type: TraceType)
    fun stopTraceWithSuccess(type: TraceType)
    fun stopTraceWithError(type: TraceType)
    fun incrementMetric(
        type: TraceType,
        metricName: String,
    )
}

enum class TraceType {
    LOAD_TRACE,
    FIRST_FRAME_TRACE,
    PLAYBACK_TRACE,
}
