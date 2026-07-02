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
    // H2H: the principal_id of whichever user inserted the row. The backend
    // stores every H2H message with role="user", so role alone can't tell us
    // which side sent it — sender_id is what the mobile client compares
    // against SessionManager.userPrincipal to render bubble alignment.
    // Defaults to null so legacy (pre-backend-patch) wire payloads and the
    // AI path (where role is authoritative) parse without breaking.
    @SerialName("sender_id")
    val senderId: String? = null,
    // When true, image attachments on this message must be blurred until the
    // user pays to unlock them. Nullable so wire payloads that don't send the
    // field keep deserializing.
    @SerialName("is_blur")
    val isBlur: Boolean? = null,
)
