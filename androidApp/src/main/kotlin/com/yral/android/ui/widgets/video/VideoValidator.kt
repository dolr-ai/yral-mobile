package com.yral.android.ui.widgets.video

import android.content.Context
import android.net.Uri
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result

class VideoValidator(
    private val videoMetadataExtractor: VideoMetadataExtractor,
) {
    // Video validation constants (matching iOS implementation)
    companion object {
        const val VIDEO_MAX_DURATION_SECONDS = 60.0
        const val VIDEO_MAX_FILE_SIZE_BYTES = 200L * 1024L * 1024L // 200 MB
    }

    fun validateVideoFromUri(
        context: Context,
        uri: Uri,
    ): Result<ValidationSuccess, ValidationError> =
        validateVideo(
            getDuration = { videoMetadataExtractor.getVideoDuration(context, uri) },
            getFileSize = { videoMetadataExtractor.getVideoFileSize(context, uri) },
        )

    fun validateVideoFromPath(filePath: String): Result<ValidationSuccess, ValidationError> =
        validateVideo(
            getDuration = { videoMetadataExtractor.getVideoDuration(filePath) },
            getFileSize = { videoMetadataExtractor.getVideoFileSize(filePath) },
        )

    @Suppress("ReturnCount")
    private fun validateVideo(
        getDuration: () -> Double?,
        getFileSize: () -> Long?,
    ): Result<ValidationSuccess, ValidationError> {
        val duration = getDuration()
        if (duration == null) {
            return Err(ValidationError.UnableToReadDuration)
        }
        if (duration > VIDEO_MAX_DURATION_SECONDS) {
            return Err(ValidationError.DurationExceedsLimit(duration, VIDEO_MAX_DURATION_SECONDS))
        }

        val fileSize = getFileSize()
        if (fileSize == null) {
            return Err(ValidationError.UnableToReadFileSize)
        }
        if (fileSize > VIDEO_MAX_FILE_SIZE_BYTES) {
            return Err(ValidationError.FileSizeExceedsLimit(fileSize, VIDEO_MAX_FILE_SIZE_BYTES))
        }

        return Ok(ValidationSuccess(duration, fileSize))
    }

    sealed class ValidationError {
        object UnableToReadDuration : ValidationError()
        object UnableToReadFileSize : ValidationError()
        data class DurationExceedsLimit(
            val actual: Double,
            val limit: Double,
        ) : ValidationError()
        data class FileSizeExceedsLimit(
            val actual: Long,
            val limit: Long,
        ) : ValidationError()
    }

    data class ValidationSuccess(
        val duration: Double,
        val fileSize: Long,
    )
}
