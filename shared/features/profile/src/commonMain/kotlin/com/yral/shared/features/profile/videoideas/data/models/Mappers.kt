package com.yral.shared.features.profile.videoideas.data.models

import com.yral.shared.features.profile.videoideas.domain.models.VideoIdea
import com.yral.shared.features.profile.videoideas.domain.models.VideoIdeaStatus

fun VideoIdeaDto.toDomain(): VideoIdea =
    VideoIdea(
        id = id,
        influencerId = influencerId,
        batchDate = batchDate,
        rank = rank,
        hook = hook,
        ideaText = ideaText,
        status = VideoIdeaStatus.fromApi(status),
        usedAt = usedAt,
    )

fun ListVideoIdeasResponseDto.toDomain(): List<VideoIdea> = ideas.map { it.toDomain() }

fun MarkVideoIdeaUsedResponseDto.toDomain(): VideoIdea = idea.toDomain()
