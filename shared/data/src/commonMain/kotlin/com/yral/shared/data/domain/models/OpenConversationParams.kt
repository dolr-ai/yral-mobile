package com.yral.shared.data.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class OpenConversationParams(
    val influencerId: String,
    val influencerCategory: String = "",
    val influencerSource: ConversationInfluencerSource = ConversationInfluencerSource.CARD,
    val conversationId: String? = null,
    val userId: String? = null,
)

@Serializable
enum class ConversationInfluencerSource {
    CARD,
    PROFILE,
}
