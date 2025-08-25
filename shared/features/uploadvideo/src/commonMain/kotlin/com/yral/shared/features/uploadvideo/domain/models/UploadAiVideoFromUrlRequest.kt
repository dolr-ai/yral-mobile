package com.yral.shared.features.uploadvideo.domain.models

internal data class UploadAiVideoFromUrlRequest(
    val videoUrl: String,
    val hashtags: List<String>,
    val description: String,
    val isNsfw: Boolean = false,
    val enableHotOrNot: Boolean = false,
)
