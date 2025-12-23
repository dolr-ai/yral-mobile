package com.yral.shared.features.chat.domain.models

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val role: ConversationMessageRole,
    val content: String?,
    val messageType: ChatMessageType,
    val mediaUrls: List<String>,
    val audioUrl: String?,
    val audioDurationSeconds: Int?,
    val tokenCount: Int?,
    val createdAt: String,
)
