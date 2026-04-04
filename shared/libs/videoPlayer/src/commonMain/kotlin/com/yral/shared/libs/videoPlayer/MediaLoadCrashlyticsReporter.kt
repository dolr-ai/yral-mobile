package com.yral.shared.libs.videoPlayer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.libs.videoplayback.MediaDescriptor
import com.yral.shared.libs.videoplayback.PlaybackEventReporter
import org.koin.compose.koinInject

private const val MEDIA_LOAD_FAILURE_PREFIX = "media_load_failure"
private const val MEDIA_TYPE_VIDEO = "video"
private const val MEDIA_TYPE_THUMBNAIL = "thumbnail"
private const val MEDIA_SOURCE_IMAGE_LOAD = "image_load"
private const val MEDIA_SOURCE_PLAYBACK = "playback"
private const val MEDIA_SOURCE_PREFETCH = "prefetch"
private const val MEDIA_PRELOAD_ERROR_REASON = "error"

internal class MediaLoadCrashlyticsReporter(
    private val crashlyticsManager: CrashlyticsManager,
) {
    fun reportThumbnailLoadFailure(
        thumbnailUrl: String,
        mediaId: String? = null,
        index: Int? = null,
        throwable: Throwable? = null,
    ) {
        reportFailure(
            mediaType = MEDIA_TYPE_THUMBNAIL,
            source = MEDIA_SOURCE_IMAGE_LOAD,
            mediaId = mediaId,
            index = index,
            url = thumbnailUrl,
            message = throwable.messageOrNull(),
            throwable = throwable,
        )
    }

    fun reportVideoPlaybackFailure(
        videoUrl: String,
        mediaId: String? = null,
        index: Int? = null,
        category: String? = null,
        code: Any? = null,
        message: String? = null,
        throwable: Throwable? = null,
    ) {
        reportFailure(
            mediaType = MEDIA_TYPE_VIDEO,
            source = MEDIA_SOURCE_PLAYBACK,
            mediaId = mediaId,
            index = index,
            url = videoUrl,
            category = category,
            code = code,
            message = message.takeIfNotBlank() ?: throwable.messageOrNull(),
            throwable = throwable,
        )
    }

    fun reportVideoPrefetchFailure(
        videoUrl: String,
        mediaId: String? = null,
        index: Int? = null,
        reason: String,
        throwable: Throwable? = null,
    ) {
        reportFailure(
            mediaType = MEDIA_TYPE_VIDEO,
            source = MEDIA_SOURCE_PREFETCH,
            mediaId = mediaId,
            index = index,
            url = videoUrl,
            category = reason,
            message = throwable.messageOrNull(),
            throwable = throwable,
        )
    }

    private fun reportFailure(
        mediaType: String,
        source: String,
        mediaId: String?,
        index: Int?,
        url: String?,
        category: String? = null,
        code: Any? = null,
        message: String? = null,
        throwable: Throwable? = null,
    ) {
        crashlyticsManager.recordException(
            mediaLoadException(
                mediaType = mediaType,
                source = source,
                mediaId = mediaId,
                index = index,
                url = url,
                category = category,
                code = code,
                message = message,
                throwable = throwable,
            ),
        )
    }
}

internal class CrashlyticsPlaybackEventReporter(
    private val delegate: PlaybackEventReporter,
    private val mediaLoadCrashlyticsReporter: MediaLoadCrashlyticsReporter,
    private val descriptorAtIndex: (Int) -> MediaDescriptor?,
) : PlaybackEventReporter by delegate {
    override fun playbackError(
        id: String,
        index: Int,
        category: String,
        code: Any,
        message: String?,
    ) {
        delegate.playbackError(id, index, category, code, message)
        val descriptor = descriptorAtIndex(index)
        mediaLoadCrashlyticsReporter.reportVideoPlaybackFailure(
            videoUrl = descriptor?.uri.orEmpty(),
            mediaId = id,
            index = index,
            category = category,
            code = code,
            message = message,
        )
    }

    override fun preloadCanceled(
        id: String,
        index: Int,
        reason: String,
    ) {
        delegate.preloadCanceled(id, index, reason)
        if (reason != MEDIA_PRELOAD_ERROR_REASON) return
        val descriptor = descriptorAtIndex(index)
        mediaLoadCrashlyticsReporter.reportVideoPrefetchFailure(
            videoUrl = descriptor?.uri.orEmpty(),
            mediaId = id,
            index = index,
            reason = reason,
        )
    }
}

internal fun PlaybackEventReporter.withCrashlytics(
    mediaLoadCrashlyticsReporter: MediaLoadCrashlyticsReporter,
    descriptorAtIndex: (Int) -> MediaDescriptor?,
): PlaybackEventReporter =
    CrashlyticsPlaybackEventReporter(
        delegate = this,
        mediaLoadCrashlyticsReporter = mediaLoadCrashlyticsReporter,
        descriptorAtIndex = descriptorAtIndex,
    )

@Composable
internal fun rememberMediaLoadCrashlyticsReporter(): MediaLoadCrashlyticsReporter {
    val crashlyticsManager = koinInject<CrashlyticsManager>()
    return remember(crashlyticsManager) {
        MediaLoadCrashlyticsReporter(crashlyticsManager)
    }
}

@Composable
internal fun MediaThumbnailImage(
    thumbnailUrl: String,
    mediaId: String? = null,
    index: Int? = null,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    contentDescription: String? = null,
    model: Any = thumbnailUrl,
) {
    val mediaLoadCrashlyticsReporter = rememberMediaLoadCrashlyticsReporter()
    AsyncImage(
        model = model,
        modifier = modifier,
        contentScale = contentScale,
        contentDescription = contentDescription,
        onState = { state ->
            if (state is AsyncImagePainter.State.Error) {
                mediaLoadCrashlyticsReporter.reportThumbnailLoadFailure(
                    thumbnailUrl = thumbnailUrl,
                    mediaId = mediaId,
                    index = index,
                    throwable = state.result.throwable,
                )
            }
        },
    )
}

internal fun mediaLoadException(
    mediaType: String,
    source: String,
    mediaId: String? = null,
    index: Int? = null,
    url: String? = null,
    category: String? = null,
    code: Any? = null,
    message: String? = null,
    throwable: Throwable? = null,
): YralException {
    val metadata =
        buildList {
            add(MEDIA_LOAD_FAILURE_PREFIX)
            add("type=$mediaType")
            add("source=$source")
            mediaId.takeIfNotBlank()?.let { add("media_id=$it") }
            index?.let { add("index=$it") }
            url.takeIfNotBlank()?.let { add("url=$it") }
            category.takeIfNotBlank()?.let { add("category=$it") }
            code?.let { add("code=$it") }
            message.takeIfNotBlank()?.let { add("message=$it") }
        }.joinToString(separator = " ")

    return YralException(
        message = metadata,
        cause = throwable,
    )
}

private fun String?.takeIfNotBlank(): String? = this?.takeIf { it.isNotBlank() }

private fun Throwable?.messageOrNull(): String? = this?.message.takeIfNotBlank()
