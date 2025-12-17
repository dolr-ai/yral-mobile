package com.yral.shared.features.chat.domain

import com.yral.shared.features.chat.domain.models.Influencer
import com.yral.shared.features.chat.domain.models.InfluencersPageResult

interface ChatRepository {
    suspend fun getInfluencersPage(
        limit: Int,
        offset: Int,
    ): InfluencersPageResult

    suspend fun getInfluencer(id: String): Influencer
}
