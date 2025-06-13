package com.yral.android.ui.screens.home.feed.performance

import com.yral.android.ui.screens.home.feed.PrefetchDownloadTrace
import com.yral.android.ui.screens.home.feed.PrefetchLoadTimeTrace
import com.yral.android.ui.screens.home.feed.VideoPerformanceFactoryProvider
import com.yral.shared.libs.firebasePerf.FirebaseOperationTrace
import com.yral.shared.libs.videoPlayer.util.PrefetchVideoListener

class PrefetchVideoListenerImpl(
    private val urlToPrefetch: String,
    private val videoId: String,
    private val onUrlReady: (url: String) -> Unit,
    override var downloadTrace: PrefetchDownloadTrace? = null,
    override var loadTrace: PrefetchLoadTimeTrace? = null,
) : PrefetchVideoListener,
    IPrefetchPerformanceMonitor {
    override fun onSetupPlayer() {
        setupPerformanceTracingForPlayer(
            urlToPrefetch = urlToPrefetch,
            videoId = videoId,
        )
    }

    override fun onBuffer() {
        stopTraceWithSuccess(PrefetchTraceType.LOAD_TRACE)
    }

    override fun onReady() {
        stopTraceWithSuccess(PrefetchTraceType.DOWNLOAD_TRACE)
        onUrlReady(urlToPrefetch)
    }

    override fun onIdle() {
        // STATE_IDLE can occur during normal cleanup or errors
        // Don't automatically treat as error - let onPlayerError handle actual errors
        // Just clean up traces without marking as error
        stopTrace(PrefetchTraceType.LOAD_TRACE)
        stopTrace(PrefetchTraceType.DOWNLOAD_TRACE)
    }

    override fun onPlayerError() {
        // Stop all traces on prefetch error
        stopTraceWithError(PrefetchTraceType.LOAD_TRACE)
        stopTraceWithError(PrefetchTraceType.DOWNLOAD_TRACE)
    }

    private fun setupPerformanceTracingForPlayer(
        urlToPrefetch: String,
        videoId: String,
    ) {
        // Start download and load traces for prefetch
        startTrace(
            PrefetchTraceType.LOAD_TRACE,
            urlToPrefetch,
            videoId,
        )
        startTrace(
            PrefetchTraceType.DOWNLOAD_TRACE,
            urlToPrefetch,
            videoId,
        )
    }

    override fun startTrace(
        type: PrefetchTraceType,
        url: String,
        videoId: String,
    ) {
        when (type) {
            PrefetchTraceType.DOWNLOAD_TRACE -> {
                downloadTrace =
                    VideoPerformanceFactoryProvider
                        .createPrefetchDownloadTrace(url, videoId)
                        .apply { start() }
            }

            PrefetchTraceType.LOAD_TRACE -> {
                loadTrace =
                    VideoPerformanceFactoryProvider
                        .createPrefetchLoadTimeTrace(url, videoId)
                        .apply { start() }
            }
        }
    }

    override fun stopTrace(type: PrefetchTraceType) {
        getTrace(type)?.stop()
        resetTrace(type)
    }

    override fun stopTraceWithSuccess(type: PrefetchTraceType) {
        getTrace(type)?.success()
        resetTrace(type)
    }

    override fun stopTraceWithError(type: PrefetchTraceType) {
        getTrace(type)?.error()
        resetTrace(type)
    }

    private fun getTrace(type: PrefetchTraceType): FirebaseOperationTrace? =
        when (type) {
            PrefetchTraceType.LOAD_TRACE -> loadTrace
            PrefetchTraceType.DOWNLOAD_TRACE -> downloadTrace
        }

    private fun resetTrace(type: PrefetchTraceType) {
        when (type) {
            PrefetchTraceType.LOAD_TRACE -> loadTrace = null
            PrefetchTraceType.DOWNLOAD_TRACE -> downloadTrace = null
        }
    }
}
