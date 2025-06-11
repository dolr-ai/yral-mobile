package com.yral.shared.libs.videoPlayer.performance

import com.yral.shared.libs.firebasePerf.FirebaseOperationTrace
import com.yral.shared.libs.videoPlayer.util.isHlsUrl
import io.ktor.http.Url

/**
 * Helper function to extract video ID from URL for Firebase Performance attributes
 * Firebase has a 100-character limit for attribute values
 */
private const val MAX_ATTRIBUTE_LENGTH = 90

@Suppress("TooGenericExceptionCaught", "SwallowedException")
private fun extractVideoId(url: String): String =
    try {
        // Parse URL using Ktor's multiplatform Url class
        val parsedUrl = Url(url)
        val pathAndQuery =
            buildString {
                // Remove leading slash and take path
                append(parsedUrl.encodedPath.removePrefix("/"))
                // Add query parameters if present
                if (parsedUrl.encodedQuery.isNotEmpty()) {
                    // append("?").append(parsedUrl.encodedQuery)
                }
            }
        // If still too long, take the last part
        if (pathAndQuery.length > MAX_ATTRIBUTE_LENGTH) {
            pathAndQuery.takeLast(MAX_ATTRIBUTE_LENGTH)
        } else {
            pathAndQuery.ifEmpty { url.takeLast(MAX_ATTRIBUTE_LENGTH) }
        }
    } catch (e: Exception) {
        // Fallback: if URL parsing fails, take last 50 characters
        url.takeLast(MAX_ATTRIBUTE_LENGTH)
    }

class PrefetchDownloadTrace(
    url: String,
) : FirebaseOperationTrace("${VideoPerformanceConstants.VIDEO_DOWNLOAD_TRACE}_prefetch") {
    init {
        putAttribute(VideoPerformanceConstants.VIDEO_ID_KEY, extractVideoId(url))
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
) : FirebaseOperationTrace("${VideoPerformanceConstants.VIDEO_LOAD_TRACE}_prefetch") {
    init {
        putAttribute(VideoPerformanceConstants.VIDEO_ID_KEY, extractVideoId(url))
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
    fun createPrefetchDownloadTrace(url: String): PrefetchDownloadTrace
    fun createPrefetchLoadTimeTrace(url: String): PrefetchLoadTimeTrace
}

object VideoPerformanceFactoryProvider : VideoPerformanceFactory {
    override fun createPrefetchDownloadTrace(url: String): PrefetchDownloadTrace = PrefetchDownloadTrace(url)
    override fun createPrefetchLoadTimeTrace(url: String): PrefetchLoadTimeTrace = PrefetchLoadTimeTrace(url)
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
