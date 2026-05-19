package com.yral.shared.features.uploadvideo.domain.models

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
data class UploadFileRequest(
    val videoUid: String,
    val postId: String = Uuid.random().toString(),
    val caption: String,
    val hashtags: List<String>,
    val isNSFW: Boolean = false,
    val creatorConsentForInclusionInHotOrNot: Boolean = true,
    val status: String = "Draft",
)
