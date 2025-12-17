package com.yral.shared.features.chat.domain

import com.yral.shared.features.chat.domain.models.Conversation
import com.yral.shared.features.chat.domain.models.ConversationsPageResult
import com.yral.shared.features.chat.domain.models.Influencer
import com.yral.shared.features.chat.domain.models.InfluencersPageResult

interface ChatRepository {
    suspend fun getInfluencersPage(
        limit: Int,
        offset: Int,
    ): InfluencersPageResult

    suspend fun getInfluencer(id: String): Influencer

    suspend fun createConversation(influencerId: String): Conversation

    suspend fun getConversationsPage(
        limit: Int,
        offset: Int,
        influencerId: String? = null,
    ): ConversationsPageResult
}
