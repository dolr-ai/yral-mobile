package com.yral.shared.features.chat.domain.models

data class SendMessageResult(
    val userMessage: ChatMessage,
    val assistantMessage: ChatMessage?,
)
