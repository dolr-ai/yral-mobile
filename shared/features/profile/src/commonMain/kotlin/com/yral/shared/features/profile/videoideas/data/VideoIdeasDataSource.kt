package com.yral.shared.features.profile.videoideas.data

import com.yral.shared.features.profile.videoideas.data.models.ListVideoIdeasResponseDto
import com.yral.shared.features.profile.videoideas.data.models.MarkVideoIdeaUsedResponseDto

interface VideoIdeasDataSource {
    suspend fun listIdeas(influencerId: String): ListVideoIdeasResponseDto

    suspend fun markIdeaUsed(
        influencerId: String,
        ideaId: String,
    ): MarkVideoIdeaUsedResponseDto
}
