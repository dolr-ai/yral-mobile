package com.yral.shared.libs.videoPlayer

import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.crashlytics.core.CrashlyticsProvider
import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.libs.videoplayback.MediaDescriptor
import com.yral.shared.libs.videoplayback.NoopPlaybackEventReporter
import com.yral.shared.libs.videoplayback.PlaybackEventReporter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MediaLoadCrashlyticsReporterTest {
    @Test
    fun reportThumbnailLoadFailure_recordsThumbnailMetadata() {
        val provider = RecordingCrashlyticsProvider()
        val reporter = MediaLoadCrashlyticsReporter(CrashlyticsManager(listOf(provider)))
        val cause = IllegalStateException("Forbidden")

        reporter.reportThumbnailLoadFailure(
            thumbnailUrl = "https://cdn-yral-sfw.yral.com/user/video_thumbnail.png",
            mediaId = "video-id",
            index = 3,
            throwable = cause,
        )

        val exception = provider.recordedExceptions.single()
        assertSame(cause, exception.cause)
        assertTrue(exception.message.orEmpty().contains("type=thumbnail"))
        assertTrue(exception.message.orEmpty().contains("source=image_load"))
        assertTrue(exception.message.orEmpty().contains("media_id=video-id"))
        assertTrue(exception.message.orEmpty().contains("index=3"))
    }

    @Test
    fun withCrashlytics_recordsPlaybackErrorsAndDelegates() {
        val provider = RecordingCrashlyticsProvider()
        val mediaLoadReporter = MediaLoadCrashlyticsReporter(CrashlyticsManager(listOf(provider)))
        val delegate = RecordingPlaybackEventReporter()

        delegate
            .withCrashlytics(mediaLoadReporter) {
                MediaDescriptor(
                    id = "video-id",
                    uri = "https://cdn-yral-sfw.yral.com/user/video-id.mp4",
                )
            }.playbackError(
                id = "video-id",
                index = 2,
                category = "ERROR_CODE_IO_BAD_HTTP_STATUS",
                code = 403,
                message = "Response code: 403",
            )

        assertEquals(1, delegate.playbackErrors.size)
        val exception = provider.recordedExceptions.single()
        assertTrue(exception.message.orEmpty().contains("type=video"))
        assertTrue(exception.message.orEmpty().contains("source=playback"))
        assertTrue(exception.message.orEmpty().contains("url=https://cdn-yral-sfw.yral.com/user/video-id.mp4"))
        assertTrue(exception.message.orEmpty().contains("code=403"))
        assertTrue(exception.message.orEmpty().contains("category=ERROR_CODE_IO_BAD_HTTP_STATUS"))
    }

    @Test
    fun withCrashlytics_onlyRecordsPrefetchFailuresForErrorReason() {
        val provider = RecordingCrashlyticsProvider()
        val mediaLoadReporter = MediaLoadCrashlyticsReporter(CrashlyticsManager(listOf(provider)))
        val delegate = RecordingPlaybackEventReporter()
        val reporter =
            delegate.withCrashlytics(mediaLoadReporter) {
                MediaDescriptor(
                    id = "video-id",
                    uri = "https://cdn-yral-sfw.yral.com/user/video-id.mp4",
                )
            }

        reporter.preloadCanceled(
            id = "video-id",
            index = 1,
            reason = "window_shift",
        )
        reporter.preloadCanceled(
            id = "video-id",
            index = 1,
            reason = "error",
        )

        assertEquals(2, delegate.preloadCanceledReasons.size)
        assertEquals(1, provider.recordedExceptions.size)
        val exception = provider.recordedExceptions.single()
        assertTrue(exception.message.orEmpty().contains("source=prefetch"))
        assertTrue(exception.message.orEmpty().contains("category=error"))
    }

    private class RecordingCrashlyticsProvider : CrashlyticsProvider {
        override val name: String = "recording"
        val recordedExceptions = mutableListOf<Exception>()

        override fun recordException(exception: Exception) {
            recordedExceptions += exception
        }

        override fun recordException(
            exception: Exception,
            type: ExceptionType,
        ) {
            recordedExceptions += exception
        }

        override fun logMessage(message: String) = Unit

        override fun setUserId(id: String) = Unit
    }

    private class RecordingPlaybackEventReporter : PlaybackEventReporter by NoopPlaybackEventReporter {
        val playbackErrors = mutableListOf<String>()
        val preloadCanceledReasons = mutableListOf<String>()

        override fun playbackError(
            id: String,
            index: Int,
            category: String,
            code: Any,
            message: String?,
        ) {
            playbackErrors += "$id|$index|$category|$code|$message"
        }

        override fun preloadCanceled(
            id: String,
            index: Int,
            reason: String,
        ) {
            preloadCanceledReasons += "$id|$index|$reason"
        }
    }
}
