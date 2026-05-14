package com.yral.shared.features.uploadvideo.domain.models

data class InProgressDraft(
    val createdAt: String,
    val modelId: String,
    val operationId: String,
    val prompt: String,
    val provider: String?,
    val status: String,
    val thumbnailUrl: String?,
)
