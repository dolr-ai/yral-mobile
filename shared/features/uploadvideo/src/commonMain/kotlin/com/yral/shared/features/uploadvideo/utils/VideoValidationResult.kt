package com.yral.shared.features.uploadvideo.utils

data class VideoValidationSuccess(
    val duration: Double,
    val fileSize: Long,
)

sealed class VideoValidationError {
    object UnableToReadDuration : VideoValidationError()
    object UnableToReadFileSize : VideoValidationError()
    data class DurationExceedsLimit(
        val actual: Double,
        val limit: Double,
    ) : VideoValidationError()

    data class FileSizeExceedsLimit(
        val actual: Long,
        val limit: Long,
    ) : VideoValidationError()
}
