package com.yral.shared.features.chat.domain.models

data class DeleteConversationResult(
    val success: Boolean,
    val message: String,
    val deletedConversationId: String,
    val deletedMessagesCount: Int,
)
