package com.yral.shared.features.tournament.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterTournamentRequestDto(
    @SerialName("tournament_id")
    val tournamentId: String,
    @SerialName("principal_id")
    val principalId: String,
    @SerialName("is_pro")
    val isPro: Boolean = false,
)
