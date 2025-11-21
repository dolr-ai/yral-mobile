package com.yral.shared.features.uploadvideo.data.remote.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class GetUploadUrlRequestDto(
    @SerialName("publisher_user_id")
    val publisherUserId: String,
)
