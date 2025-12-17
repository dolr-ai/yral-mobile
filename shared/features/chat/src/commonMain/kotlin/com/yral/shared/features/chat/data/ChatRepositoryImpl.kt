package com.yral.shared.features.chat.data

import com.yral.shared.features.chat.data.models.toDomain
import com.yral.shared.features.chat.data.models.toDomainActiveOnly
import com.yral.shared.features.chat.domain.ChatRepository
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
}
