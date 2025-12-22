package com.yral.shared.features.tournament.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TournamentVoteRequestDto(
    @SerialName("tournament_id")
    val tournamentId: String,
    @SerialName("principal_id")
    val principalId: String,
    @SerialName("video_id")
    val videoId: String,
    @SerialName("smiley_id")
    val smileyId: String,
)
