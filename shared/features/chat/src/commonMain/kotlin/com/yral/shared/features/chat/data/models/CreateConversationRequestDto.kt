package com.yral.shared.features.chat.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateConversationRequestDto(
    @SerialName("influencer_id")
    val influencerId: String,
)
