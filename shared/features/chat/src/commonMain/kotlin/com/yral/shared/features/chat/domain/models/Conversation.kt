package com.yral.shared.features.chat.domain.models

data class Conversation(
    val id: String,
    val userId: String,
    val influencer: ConversationInfluencer,
    val createdAt: String,
    val updatedAt: String,
    val messageCount: Int,
    val lastMessage: ConversationLastMessage?,
    val recentMessages: List<ChatMessage> = emptyList(),
)

data class ConversationInfluencer(
    val id: String,
    val name: String,
    val displayName: String,
    val avatarUrl: String,
    val suggestedMessages: List<String> = emptyList(),
)

data class ConversationLastMessage(
    val content: String,
    val role: ConversationMessageRole,
    val createdAt: String,
)

enum class ConversationMessageRole(
    val apiValue: String,
) {
    USER("user"),
    ASSISTANT("assistant"),
    ;

    companion object {
        fun fromApi(value: String): ConversationMessageRole =
            when (value.trim().lowercase()) {
                USER.apiValue -> USER
                ASSISTANT.apiValue -> ASSISTANT
                else -> USER
            }
    }
}
