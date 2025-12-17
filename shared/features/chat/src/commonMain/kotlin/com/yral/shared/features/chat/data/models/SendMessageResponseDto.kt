package com.yral.shared.features.chat.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SendMessageResponseDto(
    @SerialName("user_message")
    val userMessage: ChatMessageDto,
    @SerialName("assistant_message")
    val assistantMessage: ChatMessageDto? = null,
)
