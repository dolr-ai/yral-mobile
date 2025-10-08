package com.yral.shared.app.ui.screens.feed.performance

import com.yral.shared.app.ui.screens.feed.performance.VideoPerformanceConstants.FIRST_FRAME_RENDER_TRACE
import com.yral.shared.app.ui.screens.feed.performance.VideoPerformanceConstants.HLS_FORMAT
import com.yral.shared.app.ui.screens.feed.performance.VideoPerformanceConstants.PLAYBACK_TIME_TRACE
import com.yral.shared.app.ui.screens.feed.performance.VideoPerformanceConstants.PROGRESSIVE_FORMAT
import com.yral.shared.app.ui.screens.feed.performance.VideoPerformanceConstants.VIDEO_FORMAT_KEY
import com.yral.shared.app.ui.screens.feed.performance.VideoPerformanceConstants.VIDEO_ID_KEY
import com.yral.shared.app.ui.screens.feed.performance.VideoPerformanceConstants.VIDEO_LOAD_TRACE
import com.yral.shared.libs.firebasePerf.FirebaseOperationTrace
import com.yral.shared.libs.videoPlayer.model.Reels
import com.yral.shared.libs.videoPlayer.util.isHlsUrl

internal class PrefetchReadyTrace(
    reel: Reels,
) : FirebaseOperationTrace("${VideoPerformanceConstants.VIDEO_READY_TRACE}_prefetch") {
    init {
        putAttribute(VIDEO_ID_KEY, reel.videoId)
        putAttribute(
            VIDEO_FORMAT_KEY,
            if (isHlsUrl(reel.videoUrl)) {
                HLS_FORMAT
            } else {
                PROGRESSIVE_FORMAT
            },
        )
        setModule("video_player")
    }
}

internal class PrefetchLoadTimeTrace(
    reel: Reels,
) : FirebaseOperationTrace("${VIDEO_LOAD_TRACE}_prefetch") {
    init {
        putAttribute(VIDEO_ID_KEY, reel.videoId)
        putAttribute(
            VIDEO_FORMAT_KEY,
            if (isHlsUrl(reel.videoUrl)) {
                HLS_FORMAT
            } else {
                PROGRESSIVE_FORMAT
            },
        )
        setModule("video_player")
    }
}

internal class LoadTimeTrace(
    reel: Reels,
) : FirebaseOperationTrace(VIDEO_LOAD_TRACE) {
    init {
        putAttribute(VIDEO_ID_KEY, reel.videoId)
        putAttribute(
            VIDEO_FORMAT_KEY,
            if (isHlsUrl(reel.videoUrl)) {
                HLS_FORMAT
            } else {
                PROGRESSIVE_FORMAT
            },
        )
        setModule("video_player")
    }
}

internal class FirstFrameTrace(
    reel: Reels,
) : FirebaseOperationTrace(FIRST_FRAME_RENDER_TRACE) {
    init {
        putAttribute(VIDEO_ID_KEY, reel.videoId)
        putAttribute(
            VIDEO_FORMAT_KEY,
            if (isHlsUrl(reel.videoUrl)) {
                HLS_FORMAT
            } else {
                PROGRESSIVE_FORMAT
            },
        )
        setModule("video_player")
    }
}

internal class PlaybackTimeTrace(
    reel: Reels,
) : FirebaseOperationTrace(PLAYBACK_TIME_TRACE) {
    init {
        putAttribute(VIDEO_ID_KEY, reel.videoId)
        putAttribute(
            VIDEO_FORMAT_KEY,
            if (isHlsUrl(reel.videoUrl)) {
                HLS_FORMAT
            } else {
                PROGRESSIVE_FORMAT
            },
        )
        setModule("video_player")
    }
}

/**
 * Factory interface for creating video performance traces
 */
internal interface VideoPerformanceFactory {
    fun createPrefetchReadyTrace(reel: Reels): PrefetchReadyTrace
    fun createPrefetchLoadTimeTrace(reel: Reels): PrefetchLoadTimeTrace
    fun createLoadTimeTrace(reel: Reels): LoadTimeTrace
    fun createFirstFrameTrace(reel: Reels): FirstFrameTrace
    fun createPlaybackTimeTrace(reel: Reels): PlaybackTimeTrace
}

internal object VideoPerformanceFactoryProvider : VideoPerformanceFactory {
    override fun createPrefetchReadyTrace(reel: Reels): PrefetchReadyTrace =
        PrefetchReadyTrace(
            reel = reel,
        )

    override fun createPrefetchLoadTimeTrace(reel: Reels): PrefetchLoadTimeTrace =
        PrefetchLoadTimeTrace(
            reel = reel,
        )

    override fun createLoadTimeTrace(reel: Reels): LoadTimeTrace = LoadTimeTrace(reel)

    override fun createFirstFrameTrace(reel: Reels): FirstFrameTrace = FirstFrameTrace(reel)

    override fun createPlaybackTimeTrace(reel: Reels): PlaybackTimeTrace = PlaybackTimeTrace(reel)
}

/**
 * Video-specific performance metrics constants
 */
internal object VideoPerformanceConstants {
    // Video-specific trace names
    const val VIDEO_LOAD_TRACE = "VideoStartup"
    const val VIDEO_READY_TRACE = "VideoReady"
    const val FIRST_FRAME_RENDER_TRACE = "FirstFrame"
    const val PLAYBACK_TIME_TRACE = "VideoPlayback"

    // Video-specific attribute keys
    const val VIDEO_URL_KEY = "video_url"
    const val VIDEO_ID_KEY = "video_id"
    const val VIDEO_FORMAT_KEY = "video_format"

    // Video format values
    const val HLS_FORMAT = "hls"
    const val PROGRESSIVE_FORMAT = "progressive"

    enum class VideoPerformanceMetric(
        val key: String,
    ) {
        // Video-specific metric names
        BUFFERING_COUNT("buffering_count"),
    }
}
