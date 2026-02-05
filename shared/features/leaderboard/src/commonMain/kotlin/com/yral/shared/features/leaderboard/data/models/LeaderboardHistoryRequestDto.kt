package com.yral.shared.features.leaderboard.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LeaderboardHistoryRequestDto(
    @SerialName("principal_id")
    val principalId: String,
    @SerialName("country_code")
    val countryCode: String,
    @SerialName("game_type")
    val gameType: String = "smiley",
)
