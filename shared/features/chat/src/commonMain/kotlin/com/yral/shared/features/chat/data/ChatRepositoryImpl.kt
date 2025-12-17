package com.yral.shared.features.chat.data

import com.yral.shared.features.chat.data.models.toDomain
import com.yral.shared.features.chat.data.models.toDomainActiveOnly
import com.yral.shared.features.chat.domain.ChatRepository
import com.yral.shared.features.chat.domain.models.Conversation
import com.yral.shared.features.chat.domain.models.ConversationsPageResult
import com.yral.shared.features.chat.domain.models.DeleteConversationResult
import com.yral.shared.features.chat.domain.models.Influencer
import com.yral.shared.features.chat.domain.models.InfluencersPageResult

class ChatRepositoryImpl(
    private val dataSource: ChatDataSource,
) : ChatRepository {
    override suspend fun getInfluencersPage(
        limit: Int,
        offset: Int,
    ): InfluencersPageResult =
        dataSource
            .listInfluencers(limit = limit, offset = offset)
            .toDomainActiveOnly()

    override suspend fun getInfluencer(id: String): Influencer =
        dataSource
            .getInfluencer(id)
            .toDomain()

    override suspend fun createConversation(influencerId: String): Conversation =
        dataSource
            .createConversation(influencerId)
            .toDomain()

    override suspend fun getConversationsPage(
        limit: Int,
        offset: Int,
        influencerId: String?,
    ): ConversationsPageResult =
        dataSource
            .listConversations(
                limit = limit,
                offset = offset,
                influencerId = influencerId,
            ).toDomain()

    override suspend fun deleteConversation(conversationId: String): DeleteConversationResult =
        dataSource
            .deleteConversation(conversationId)
            .toDomain()
}
