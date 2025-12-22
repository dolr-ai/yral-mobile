package com.yral.shared.features.tournament.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TournamentErrorDto(
    @SerialName("code")
    val code: String,
    @SerialName("message")
    val message: String,
)
