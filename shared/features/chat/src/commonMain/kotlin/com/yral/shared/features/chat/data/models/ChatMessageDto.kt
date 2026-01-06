package com.yral.shared.features.chat.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatMessageDto(
    @SerialName("id")
    val id: String,
    @SerialName("conversation_id")
    val conversationId: String? = null,
    @SerialName("role")
    val role: String,
    @SerialName("content")
    val content: String? = null,
    @SerialName("message_type")
    val messageType: String,
    @SerialName("media_urls")
    val mediaUrls: List<String>? = null,
    @SerialName("audio_url")
    val audioUrl: String? = null,
    @SerialName("audio_duration_seconds")
    val audioDurationSeconds: Int? = null,
    @SerialName("token_count")
    val tokenCount: Int? = null,
    @SerialName("created_at")
    val createdAt: String,
)
