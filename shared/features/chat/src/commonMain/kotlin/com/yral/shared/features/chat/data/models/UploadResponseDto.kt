package com.yral.shared.features.chat.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UploadResponseDto(
    @SerialName("url")
    val url: String,
    @SerialName("storage_key")
    val storageKey: String,
    @SerialName("type")
    val type: String? = null,
    @SerialName("size")
    val size: Long? = null,
    @SerialName("mime_type")
    val mimeType: String? = null,
    @SerialName("uploaded_at")
    val uploadedAt: String? = null,
)
