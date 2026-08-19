package com.yral.shared.features.auth.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateAiAccountRequestDto(
    @SerialName("user_id") val userId: String,
)
