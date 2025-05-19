package com.yral.android.ui.screens.feed.performance

import co.touchlab.kermit.Logger
import com.yral.shared.libs.firebasePerf.FirebaseOperationTrace
import com.yral.shared.libs.videoPlayer.model.Reels
import com.yral.shared.libs.videoPlayer.util.PrefetchVideoListener

class PrefetchVideoListenerImpl(
    private val reelToPrefetch: Reels,
    override var readyTrace: PrefetchReadyTrace? = null,
    override var loadTrace: PrefetchLoadTimeTrace? = null,
) : PrefetchVideoListener,
    IPrefetchPerformanceMonitor {
    private val enablePerformanceTracing: Boolean = true

    override fun onSetupPlayer() {
        setupPerformanceTracingForPlayer()
    }

    override fun onBuffer() {
        stopTraceWithSuccess(PrefetchTraceType.LOAD_TRACE)
    }

    override fun onReady() {
        stopTraceWithSuccess(PrefetchTraceType.READY_TRACE)
    }

    override fun onIdle() {
        // STATE_IDLE can occur during normal cleanup or errors
        // Don't automatically treat as error - let onPlayerError handle actual errors
        // Just clean up traces without marking as error
        stopTrace(PrefetchTraceType.LOAD_TRACE)
        stopTrace(PrefetchTraceType.READY_TRACE)
    }

    override fun onPlayerError() {
        // Stop all traces on prefetch error
        stopTraceWithError(PrefetchTraceType.LOAD_TRACE)
        stopTraceWithError(PrefetchTraceType.READY_TRACE)
    }

    private fun setupPerformanceTracingForPlayer() {
        // Start download and load traces for prefetch
        startTrace(PrefetchTraceType.LOAD_TRACE, reelToPrefetch)
        startTrace(PrefetchTraceType.READY_TRACE, reelToPrefetch)
    }

    override fun startTrace(
        type: PrefetchTraceType,
        reel: Reels,
    ) {
        if (!enablePerformanceTracing) return
        Logger.d("Logging trace metric: startTrace $type for ${reel.videoId}")
        when (type) {
            PrefetchTraceType.READY_TRACE -> {
                readyTrace =
                    VideoPerformanceFactoryProvider.createPrefetchReadyTrace(reel)
                        .apply { start() }
            }

            PrefetchTraceType.LOAD_TRACE -> {
                loadTrace =
                    VideoPerformanceFactoryProvider.createPrefetchLoadTimeTrace(reel)
                        .apply { start() }
            }
        }
    }

    override fun stopTrace(type: PrefetchTraceType) {
        getTrace(type)?.let {
            logTrace("stopTrace", type)
            it.stop()
            resetTrace(type)
        }
    }

    override fun stopTraceWithSuccess(type: PrefetchTraceType) {
        getTrace(type)?.let {
            logTrace("stopTraceWithSuccess", type)
            it.success()
            resetTrace(type)
        }
    }

    override fun stopTraceWithError(type: PrefetchTraceType) {
        getTrace(type)?.let {
            logTrace("stopTraceWithError", type)
            it.error()
            resetTrace(type)
        }
    }

    private fun logTrace(
        methodName: String,
        type: PrefetchTraceType,
    ) {
        getTrace(type)?.let {
            Logger.d("Logging trace metric: $methodName $type for ${reelToPrefetch.videoId}")
        }
    }

    private fun getTrace(type: PrefetchTraceType): FirebaseOperationTrace? =
        when (type) {
            PrefetchTraceType.LOAD_TRACE -> loadTrace
            PrefetchTraceType.READY_TRACE -> readyTrace
        }

    private fun resetTrace(type: PrefetchTraceType) {
        when (type) {
            PrefetchTraceType.LOAD_TRACE -> loadTrace = null
            PrefetchTraceType.READY_TRACE -> readyTrace = null
        }
    }
}
