package com.yral.shared.features.tournament.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VideoEmojisRequestDto(
    @SerialName("tournament_id")
    val tournamentId: String,
    @SerialName("video_id")
    val videoId: String,
)
