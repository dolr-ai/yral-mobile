package com.yral.shared.features.chat.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConversationsResponseDto(
    @SerialName("conversations")
    val conversations: List<ConversationDto>,
    @SerialName("total")
    val total: Int,
    @SerialName("limit")
    val limit: Int,
    @SerialName("offset")
    val offset: Int,
)
