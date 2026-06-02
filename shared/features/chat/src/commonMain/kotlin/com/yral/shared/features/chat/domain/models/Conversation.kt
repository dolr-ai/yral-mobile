package com.yral.shared.features.chat.domain.models

data class Conversation(
    val id: String,
    val userId: String,
    // Nullable on H2H conversations (where the other party is a user, not
    // an influencer). Always present on AI / chat-as-human conversations.
    // Callers branch on [conversationType] to know which case they're in.
    val influencer: ConversationInfluencer?,
    val conversationUser: ConversationUser? = null,
    val createdAt: String,
    val updatedAt: String,
    val messageCount: Int,
    val lastMessage: ConversationLastMessage?,
    val recentMessages: List<ChatMessage> = emptyList(),
    val unreadCount: Int = 0,
    // Raw backend type discriminator: "human_chat" for H2H, "ai_chat" or
    // null for the existing influencer-backed conversations. Mappers
    // populate this from the wire; UI/VM branch off it (alongside the
    // presence of `conversationUser` / `influencer`) to pick the right
    // header, send path, and visual affordances.
    val conversationType: String? = null,
)

const val HUMAN_CHAT_CONVERSATION_TYPE = "human_chat"

val Conversation.isHumanChat: Boolean
    get() = conversationType == HUMAN_CHAT_CONVERSATION_TYPE || conversationUser != null

data class ConversationUser(
    val principalId: String,
    val username: String?,
    val profilePictureUrl: String?,
)

data class ConversationInfluencer(
    val id: String,
    val name: String,
    val displayName: String,
    val avatarUrl: String,
    val category: String = "",
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
    SYSTEM("system"),
    ;

    companion object {
        fun fromApi(value: String): ConversationMessageRole =
            when (value.trim().lowercase()) {
                USER.apiValue -> USER
                ASSISTANT.apiValue -> ASSISTANT
                SYSTEM.apiValue -> SYSTEM
                else -> USER
            }
    }
}
