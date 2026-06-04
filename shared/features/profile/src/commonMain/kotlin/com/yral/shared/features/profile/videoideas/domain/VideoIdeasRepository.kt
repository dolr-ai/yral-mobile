package com.yral.shared.features.profile.videoideas.domain

import com.yral.shared.features.profile.videoideas.domain.models.VideoIdea

interface VideoIdeasRepository {
    suspend fun listIdeas(influencerId: String): List<VideoIdea>

    suspend fun markIdeaUsed(
        influencerId: String,
        ideaId: String,
    ): VideoIdea
}
