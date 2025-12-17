package com.yral.shared.features.chat.data

import com.yral.shared.features.chat.data.models.InfluencerDto
import com.yral.shared.features.chat.data.models.InfluencersResponseDto

interface ChatDataSource {
    suspend fun listInfluencers(
        limit: Int,
        offset: Int,
    ): InfluencersResponseDto

    suspend fun getInfluencer(id: String): InfluencerDto
}
