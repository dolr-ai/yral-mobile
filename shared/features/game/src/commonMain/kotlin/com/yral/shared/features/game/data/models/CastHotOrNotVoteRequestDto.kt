package com.yral.shared.features.game.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CastHotOrNotVoteRequestDto(
    @SerialName("principal_id")
    val principalId: String,
    @SerialName("video_id")
    val videoId: String,
    @SerialName("vote")
    val vote: String,
)
