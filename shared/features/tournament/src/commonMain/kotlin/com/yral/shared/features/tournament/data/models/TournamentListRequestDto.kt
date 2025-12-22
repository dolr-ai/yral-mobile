package com.yral.shared.features.tournament.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TournamentListRequestDto(
    @SerialName("date")
    val date: String? = null,
    @SerialName("status")
    val status: String? = null,
)
