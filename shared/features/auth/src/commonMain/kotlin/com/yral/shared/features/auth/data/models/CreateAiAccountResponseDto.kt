package com.yral.shared.features.auth.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateAiAccountResponseDto(
    @SerialName("ai_account_id") val aiAccountId: String,
)
