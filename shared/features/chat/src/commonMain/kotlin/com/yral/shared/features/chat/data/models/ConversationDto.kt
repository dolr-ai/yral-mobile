package com.yral.shared.features.chat.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConversationDto(
    @SerialName("id")
    val id: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("influencer_id")
    val influencerId: String? = null,
    @SerialName("influencer")
    val influencer: ConversationInfluencerDto? = null,
    @SerialName("user")
    val user: ConversationUserDto? = null,
    // H2H plumbing: backend exposes "human_chat" here for direct-message
    // conversations and "ai_chat" (or null on pre-Day-8 rows) for the
    // existing influencer-backed chats. Mappers branch on this to decide
    // whether `influencer` may legally be null in the domain.
    @SerialName("conversation_type")
    val conversationType: String? = null,
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
    @SerialName("unread_count")
    val unreadCount: Int = 0,
)

@Serializable
data class ConversationUserDto(
    @SerialName("principal_id")
    val principalId: String,
    @SerialName("username")
    val username: String? = null,
    @SerialName("profile_picture_url")
    val profilePictureUrl: String? = null,
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
    @SerialName("category")
    val category: String? = null,
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
