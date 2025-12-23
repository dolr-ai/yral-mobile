package com.yral.shared.features.chat.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConversationDto(
    @SerialName("id")
    val id: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("influencer")
    val influencer: ConversationInfluencerDto,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
    @SerialName("message_count")
    val messageCount: Int,
    @SerialName("last_message")
    val lastMessage: ConversationLastMessageDto? = null,
    @SerialName("recent_messages")
    val recentMessages: List<ChatMessageDto>? = null,
)

@Serializable
data class ConversationInfluencerDto(
    @SerialName("id")
    val id: String,
    @SerialName("name")
    val name: String,
    @SerialName("display_name")
    val displayName: String,
    @SerialName("avatar_url")
    val avatarUrl: String,
    @SerialName("suggested_messages")
    val suggestedMessages: List<String>? = null,
)

@Serializable
data class ConversationLastMessageDto(
    @SerialName("content")
    val content: String,
    @SerialName("role")
    val role: String,
    @SerialName("created_at")
    val createdAt: String,
)
