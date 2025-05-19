package com.yral.android.ui.screens.feed.performance

import co.touchlab.kermit.Logger
import com.yral.shared.libs.firebasePerf.FirebaseOperationTrace
import com.yral.shared.libs.videoPlayer.model.Reels
import com.yral.shared.libs.videoPlayer.pool.VideoListener

class VideoListenerImpl(
    private val reel: Reels,
    val registerTrace: (videoId: String, traceType: TraceType) -> Unit,
    val isTraced: (videoId: String, traceType: TraceType) -> Boolean,
    override var loadTrace: LoadTimeTrace? = null,
    override var firstFrameTrace: FirstFrameTrace? = null,
    override var playbackTimeTrace: PlaybackTimeTrace? = null,
) : VideoListener,
    PerformanceMonitor {
    private var isPlayBackTraced: Boolean = false
    private val enablePerformanceTracing: Boolean = true

    override fun onSetupPlayer() {
        startTrace(TraceType.LOAD_TRACE)
        startTrace(TraceType.FIRST_FRAME_TRACE)
    }

    override fun onBuffer() {
        // Stop load trace when buffering starts (decoder initialized)
        stopTraceWithSuccess(TraceType.LOAD_TRACE)
        incrementMetric(
            type = TraceType.PLAYBACK_TRACE,
            metricName = VideoPerformanceConstants.VideoPerformanceMetric.BUFFERING_COUNT.key,
        )
    }

    override fun onReady() {
        // Stop first frame trace when ready (network data received + decoded)
        stopTraceWithSuccess(TraceType.FIRST_FRAME_TRACE)
    }

    override fun onIdle() {
        // Clean up traces without marking as error (normal cleanup)
        stopTrace(TraceType.LOAD_TRACE)
        stopTrace(TraceType.FIRST_FRAME_TRACE)
        stopTrace(TraceType.PLAYBACK_TRACE)
    }

    override fun onEnd() {
        stopTraceWithSuccess(TraceType.PLAYBACK_TRACE)
        isPlayBackTraced = true
    }

    override fun onPlayerError() {
        // Mark traces as failed on error
        stopTraceWithError(TraceType.LOAD_TRACE)
        stopTraceWithError(TraceType.FIRST_FRAME_TRACE)
        stopTraceWithError(TraceType.PLAYBACK_TRACE)
    }

    override fun onPlayBackStarted() {
        if (!isPlayBackTraced) {
            startTrace(TraceType.PLAYBACK_TRACE)
        }
    }

    override fun onPlayBackStopped() {
        stopTrace(TraceType.PLAYBACK_TRACE)
    }

    override fun startTrace(type: TraceType) {
        if (!enablePerformanceTracing || isTraced(reel.videoId, type)) return
        Logger.d("Logging trace metric: startTrace $type for ${reel.videoId}")
        when (type) {
            TraceType.LOAD_TRACE -> {
                loadTrace =
                    VideoPerformanceFactoryProvider.createLoadTimeTrace(reel)
                        .apply { start() }
            }

            TraceType.FIRST_FRAME_TRACE -> {
                firstFrameTrace =
                    VideoPerformanceFactoryProvider.createFirstFrameTrace(reel)
                        .apply { start() }
            }

            TraceType.PLAYBACK_TRACE -> {
                if (playbackTimeTrace != null) return
                playbackTimeTrace =
                    VideoPerformanceFactoryProvider.createPlaybackTimeTrace(reel)
                        .apply { start() }
            }
        }
    }

    override fun stopTrace(type: TraceType) {
        getTrace(type)?.let {
            logTrace("stopTrace", type)
            it.stop()
            resetTrace(type)
        }
    }

    override fun stopTraceWithSuccess(type: TraceType) {
        getTrace(type)?.let {
            logTrace("stopTraceWithSuccess", type)
            registerTrace(reel.videoId, type)
            it.success()
            resetTrace(type)
        }
    }

    override fun stopTraceWithError(type: TraceType) {
        getTrace(type)?.let {
            logTrace("stopTraceWithError", type)
            it.error()
            resetTrace(type)
        }
    }

    override fun incrementMetric(
        type: TraceType,
        metricName: String,
    ) {
        getTrace(type)?.incrementMetric(metricName, 1)
    }

    private fun logTrace(
        methodName: String,
        type: TraceType,
    ) {
        getTrace(type)?.let {
            Logger.d("Logging trace metric: $methodName $type for ${reel.videoId}")
        }
    }

    private fun getTrace(type: TraceType): FirebaseOperationTrace? =
        when (type) {
            TraceType.LOAD_TRACE -> loadTrace
            TraceType.FIRST_FRAME_TRACE -> firstFrameTrace
            TraceType.PLAYBACK_TRACE -> playbackTimeTrace
        }

    private fun resetTrace(type: TraceType) =
        when (type) {
            TraceType.LOAD_TRACE -> loadTrace = null
            TraceType.FIRST_FRAME_TRACE -> firstFrameTrace = null
            TraceType.PLAYBACK_TRACE -> playbackTimeTrace = null
        }
}
