package com.yral.shared.features.profile.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UploadProfileImageRequestBody(
    @SerialName("image_data")
    val imageData: String,
)

@Serializable
data class UploadProfileImageResponse(
    @SerialName("profile_image_url")
    val profileImageUrl: String,
)
