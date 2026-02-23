package com.yral.shared.libs.videoplayback

import com.yral.shared.libs.firebasePerf.FirebasePerformanceTrace
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Suppress("TooManyFunctions")
class FirebasePerfPlaybackReporter(
    private val delegate: PlaybackEventReporter,
) : PlaybackEventReporter {
    private var activeTrace: FirebasePerformanceTrace? = null
    private var impressionMs: Long = 0
    private var rebufferStartMs: Long = 0
    private var totalRebufferMs: Long = 0
    private var rebufferCount: Int = 0
    private var stallStartMs: Long = 0
    private var totalStallMs: Long = 0
    private var stallCount: Int = 0

    @OptIn(ExperimentalTime::class)
    private fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()

    override fun playStartRequest(
        id: String,
        index: Int,
        reason: String,
    ) {
        stopCurrentTrace()
        startNewTrace(id)
        delegate.playStartRequest(id, index, reason)
    }

    override fun timeToFirstFrame(
        id: String,
        index: Int,
        ms: Long,
    ) {
        activeTrace?.putMetric("time_to_first_frame_ms", ms)
        delegate.timeToFirstFrame(id, index, ms)
    }

    override fun rebufferStart(
        id: String,
        index: Int,
        reason: String,
    ) {
        rebufferCount++
        rebufferStartMs = nowMs()
        delegate.rebufferStart(id, index, reason)
    }

    override fun rebufferEnd(
        id: String,
        index: Int,
        reason: String,
    ) {
        if (rebufferStartMs > 0) {
            totalRebufferMs += nowMs() - rebufferStartMs
            rebufferStartMs = 0
        }
        delegate.rebufferEnd(id, index, reason)
    }

    override fun stallStart(
        id: String,
        index: Int,
        reason: String,
    ) {
        stallCount++
        stallStartMs = nowMs()
        delegate.stallStart(id, index, reason)
    }

    override fun stallEnd(
        id: String,
        index: Int,
        durationMs: Long,
    ) {
        totalStallMs += durationMs
        stallStartMs = 0
        delegate.stallEnd(id, index, durationMs)
    }

    override fun videoFullyBuffered(
        id: String,
        index: Int,
        ms: Long,
    ) {
        activeTrace?.putMetric("video_fully_buffered_ms", ms)
        delegate.videoFullyBuffered(id, index, ms)
    }

    override fun feedItemImpression(
        id: String,
        index: Int,
    ) {
        impressionMs = nowMs()
        delegate.feedItemImpression(id, index)
    }

    override fun firstFrameRendered(
        id: String,
        index: Int,
    ) {
        if (impressionMs > 0) {
            activeTrace?.putMetric("viewport_to_play_ms", nowMs() - impressionMs)
            impressionMs = 0
        }
        delegate.firstFrameRendered(id, index)
    }

    override fun playbackProgress(
        id: String,
        index: Int,
        positionMs: Long,
        durationMs: Long,
    ) = delegate.playbackProgress(id, index, positionMs, durationMs)

    override fun rebufferTotal(
        id: String,
        index: Int,
        ms: Long,
    ) = delegate.rebufferTotal(id, index, ms)

    override fun playbackError(
        id: String,
        index: Int,
        category: String,
        code: Any,
        message: String?,
    ) = delegate.playbackError(id, index, category, code, message)

    override fun playbackEnded(
        id: String,
        index: Int,
    ) = delegate.playbackEnded(id, index)

    override fun preloadScheduled(
        id: String,
        index: Int,
        distance: Int,
        mode: String,
    ) = delegate.preloadScheduled(id, index, distance, mode)

    override fun preloadCompleted(
        id: String,
        index: Int,
        bytes: Long,
        ms: Long,
        fromCache: Boolean,
    ) = delegate.preloadCompleted(id, index, bytes, ms, fromCache)

    override fun preloadCanceled(
        id: String,
        index: Int,
        reason: String,
    ) = delegate.preloadCanceled(id, index, reason)

    override fun cacheHit(
        id: String,
        bytes: Long,
    ) = delegate.cacheHit(id, bytes)

    override fun cacheMiss(
        id: String,
        bytes: Long,
    ) = delegate.cacheMiss(id, bytes)

    private fun stopCurrentTrace() {
        activeTrace?.let { trace ->
            trace.putMetric("rebuffer_count", rebufferCount.toLong())
            trace.putMetric("total_rebuffer_duration_ms", totalRebufferMs)
            trace.putMetric("stall_count", stallCount.toLong())
            trace.putMetric("total_stall_duration_ms", totalStallMs)
            trace.stop()
        }
        resetState()
    }

    private fun startNewTrace(videoId: String) {
        activeTrace =
            FirebasePerformanceTrace(TRACE_NAME).apply {
                putAttribute("video_id", videoId)
                start()
            }
    }

    private fun resetState() {
        activeTrace = null
        impressionMs = 0
        rebufferStartMs = 0
        totalRebufferMs = 0
        rebufferCount = 0
        stallStartMs = 0
        totalStallMs = 0
        stallCount = 0
    }

    private companion object {
        private const val TRACE_NAME = "VideoPlaybackSession"
    }
}
