package com.yral.shared.features.chat.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConversationMessagesResponseDto(
    @SerialName("conversation_id")
    val conversationId: String,
    @SerialName("messages")
    val messages: List<ChatMessageDto>,
    @SerialName("total")
    val total: Int,
    @SerialName("limit")
    val limit: Int,
    @SerialName("offset")
    val offset: Int,
)
