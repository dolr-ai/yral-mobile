package com.yral.android.ui.screens.home.feed

import com.yral.shared.libs.firebasePerf.FirebaseOperationTrace
import com.yral.shared.libs.videoPlayer.util.isHlsUrl

class PrefetchDownloadTrace(
    url: String,
    videoId: String,
) : FirebaseOperationTrace("${VideoPerformanceConstants.VIDEO_DOWNLOAD_TRACE}_prefetch") {
    init {
        putAttribute(VideoPerformanceConstants.VIDEO_ID_KEY, videoId)
        putAttribute(
            VideoPerformanceConstants.VIDEO_FORMAT_KEY,
            if (isHlsUrl(url)) {
                VideoPerformanceConstants.HLS_FORMAT
            } else {
                VideoPerformanceConstants.PROGRESSIVE_FORMAT
            },
        )
        setModule("video_player")
    }
}

class PrefetchLoadTimeTrace(
    url: String,
    videoId: String,
) : FirebaseOperationTrace("${VideoPerformanceConstants.VIDEO_LOAD_TRACE}_prefetch") {
    init {
        putAttribute(VideoPerformanceConstants.VIDEO_ID_KEY, videoId)
        putAttribute(
            VideoPerformanceConstants.VIDEO_FORMAT_KEY,
            if (isHlsUrl(url)) {
                VideoPerformanceConstants.HLS_FORMAT
            } else {
                VideoPerformanceConstants.PROGRESSIVE_FORMAT
            },
        )
        setModule("video_player")
    }
}

/**
 * Factory interface for creating video performance traces
 */
interface VideoPerformanceFactory {
    fun createPrefetchDownloadTrace(
        url: String,
        videoId: String,
    ): PrefetchDownloadTrace

    fun createPrefetchLoadTimeTrace(
        url: String,
        videoId: String,
    ): PrefetchLoadTimeTrace
}

object VideoPerformanceFactoryProvider : VideoPerformanceFactory {
    override fun createPrefetchDownloadTrace(
        url: String,
        videoId: String,
    ): PrefetchDownloadTrace = PrefetchDownloadTrace(url, videoId)

    override fun createPrefetchLoadTimeTrace(
        url: String,
        videoId: String,
    ): PrefetchLoadTimeTrace = PrefetchLoadTimeTrace(url, videoId)
}

/**
 * Video-specific performance metrics constants
 */
object VideoPerformanceConstants {
    // Video-specific trace names
    const val VIDEO_DOWNLOAD_TRACE = "VideoDownload"
    const val VIDEO_LOAD_TRACE = "VideoStartup"

    // Video-specific attribute keys
    const val VIDEO_URL_KEY = "video_url"
    const val VIDEO_ID_KEY = "video_id"
    const val VIDEO_FORMAT_KEY = "video_format"
    const val PLAYER_STATE_KEY = "player_state"

    // Video format values
    const val HLS_FORMAT = "hls"
    const val PROGRESSIVE_FORMAT = "progressive"

    // Video-specific metric names
    const val DOWNLOAD_TIME_MS = "download_time_ms"
    const val LOAD_TIME_MS = "load_time_ms"
    const val FIRST_FRAME_TIME_MS = "first_frame_time_ms"
    const val BUFFER_TIME_MS = "buffer_time_ms"
    const val PLAYBACK_TIME_MS = "playback_time_ms"
    const val BUFFERING_COUNT = "buffering_count"
}
