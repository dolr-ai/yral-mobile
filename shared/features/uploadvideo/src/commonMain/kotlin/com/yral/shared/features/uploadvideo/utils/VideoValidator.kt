package com.yral.shared.features.uploadvideo.utils

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result

class VideoValidator {
    // Video validation constants (matching iOS implementation)
    companion object Companion {
        const val VIDEO_MAX_DURATION_SECONDS = 60.0
        const val VIDEO_MAX_FILE_SIZE_BYTES = 200L * 1024L * 1024L // 200 MB
    }

    @Suppress("ReturnCount")
    internal fun validateVideo(
        getDuration: () -> Double?,
        getFileSize: () -> Long?,
    ): Result<VideoValidationSuccess, VideoValidationError> {
        val duration = getDuration()
        if (duration == null) {
            return Err(VideoValidationError.UnableToReadDuration)
        }
        if (duration > VIDEO_MAX_DURATION_SECONDS) {
            return Err(VideoValidationError.DurationExceedsLimit(duration, VIDEO_MAX_DURATION_SECONDS))
        }

        val fileSize = getFileSize()
        if (fileSize == null) {
            return Err(VideoValidationError.UnableToReadFileSize)
        }
        if (fileSize > VIDEO_MAX_FILE_SIZE_BYTES) {
            return Err(VideoValidationError.FileSizeExceedsLimit(fileSize, VIDEO_MAX_FILE_SIZE_BYTES))
        }

        return Ok(VideoValidationSuccess(duration, fileSize))
    }
}
