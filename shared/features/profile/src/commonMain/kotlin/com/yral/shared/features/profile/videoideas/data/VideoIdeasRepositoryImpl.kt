package com.yral.shared.features.profile.videoideas.data

import com.yral.shared.features.profile.videoideas.data.models.toDomain
import com.yral.shared.features.profile.videoideas.domain.VideoIdeasRepository
import com.yral.shared.features.profile.videoideas.domain.models.VideoIdea

class VideoIdeasRepositoryImpl(
    private val dataSource: VideoIdeasDataSource,
) : VideoIdeasRepository {
    @Suppress("MaxLineLength")
    override suspend fun listIdeas(influencerId: String): List<VideoIdea> = dataSource.listIdeas(influencerId).toDomain()

    override suspend fun markIdeaUsed(
        influencerId: String,
        ideaId: String,
    ): VideoIdea = dataSource.markIdeaUsed(influencerId, ideaId).toDomain()
}
