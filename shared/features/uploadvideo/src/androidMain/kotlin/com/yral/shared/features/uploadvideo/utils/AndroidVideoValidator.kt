package com.yral.shared.features.uploadvideo.utils

import android.content.Context
import android.net.Uri
import com.github.michaelbull.result.Result

class AndroidVideoValidator(
    private val videoMetadataExtractor: VideoMetadataExtractor,
    private val videoValidator: VideoValidator,
) {
    fun validateVideoFromUri(
        context: Context,
        uri: Uri,
    ): Result<VideoValidationSuccess, VideoValidationError> =
        videoValidator.validateVideo(
            getDuration = { videoMetadataExtractor.getVideoDuration(context, uri) },
            getFileSize = { videoMetadataExtractor.getVideoFileSize(context, uri) },
        )

    fun validateVideoFromPath(filePath: String): Result<VideoValidationSuccess, VideoValidationError> =
        videoValidator.validateVideo(
            getDuration = { videoMetadataExtractor.getVideoDuration(filePath) },
            getFileSize = { videoMetadataExtractor.getVideoFileSize(filePath) },
        )
}
