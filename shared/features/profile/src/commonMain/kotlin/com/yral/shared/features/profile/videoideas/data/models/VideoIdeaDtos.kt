package com.yral.shared.features.profile.videoideas.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VideoIdeaDto(
    @SerialName("id") val id: String,
    @SerialName("influencer_id") val influencerId: String,
    @SerialName("batch_date") val batchDate: String,
    @SerialName("rank") val rank: Int,
    @SerialName("hook") val hook: String,
    @SerialName("idea_text") val ideaText: String,
    @SerialName("status") val status: String,
    @SerialName("used_at") val usedAt: String? = null,
)

@Serializable
data class ListVideoIdeasResponseDto(
    @SerialName("influencer_id") val influencerId: String,
    @SerialName("ideas") val ideas: List<VideoIdeaDto>,
    @SerialName("total") val total: Int,
)

@Serializable
data class MarkVideoIdeaUsedResponseDto(
    @SerialName("idea") val idea: VideoIdeaDto,
)
