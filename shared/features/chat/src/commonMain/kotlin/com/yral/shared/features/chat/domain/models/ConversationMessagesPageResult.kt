package com.yral.shared.features.chat.domain.models

data class ConversationMessagesPageResult(
    val conversationId: String,
    val messages: List<ChatMessage>,
    val total: Int,
    val limit: Int,
    val offset: Int,
    val nextOffset: Int?,
    val rawCount: Int,
)
