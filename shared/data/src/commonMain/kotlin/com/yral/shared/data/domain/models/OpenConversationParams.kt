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
)

@Serializable
enum class ConversationInfluencerSource {
    CARD,
    PROFILE,
}
