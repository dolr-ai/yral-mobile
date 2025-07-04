package com.yral.shared.features.uploadvideo.domain.models

internal data class UploadFileRequest(
    val videoUid: String,
    val caption: String,
    val hashtags: List<String>,
    val isNSFW: Boolean = false,
    val creatorConsentForInclusionInHotOrNot: Boolean = true,
)
