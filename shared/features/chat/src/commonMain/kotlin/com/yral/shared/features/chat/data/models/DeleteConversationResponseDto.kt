package com.yral.shared.features.chat.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeleteConversationResponseDto(
    @SerialName("success")
    val success: Boolean,
    @SerialName("message")
    val message: String,
    @SerialName("deleted_conversation_id")
    val deletedConversationId: String,
    @SerialName("deleted_messages_count")
    val deletedMessagesCount: Int,
)
