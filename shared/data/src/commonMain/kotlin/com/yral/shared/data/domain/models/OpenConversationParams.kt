package com.yral.shared.data.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class OpenConversationParams(
    val influencerId: String,
    val influencerCategory: String = "",
    val influencerSource: ConversationInfluencerSource = ConversationInfluencerSource.CARD,
    val conversationId: String? = null,
    val userId: String? = null,
    val username: String? = null,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val autoTriggerPurchase: Boolean = false,
    // H2H: the other-user's principal_id, populated when this navigation
    // comes from a "Send Message" tap on a user profile. Null for AI
    // chats. The chat screen also derives the H2H vs AI mode from the
    // loaded Conversation.conversationType, so this field is a UX hint
    // (for empty-state rendering before the network load completes)
    // rather than the canonical signal.
    val participantPrincipalId: String? = null,
)

@Serializable
enum class ConversationInfluencerSource {
    CARD,
    PROFILE,
}
