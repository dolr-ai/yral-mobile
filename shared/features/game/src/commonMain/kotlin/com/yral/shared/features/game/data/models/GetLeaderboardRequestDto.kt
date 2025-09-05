package com.yral.shared.features.game.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetLeaderboardRequestDto(
    @SerialName("principal_id")
    val principalId: String,
    @SerialName("mode")
    val mode: LeaderboardMode,
)

@Serializable
enum class LeaderboardMode(
    val showCountDown: Boolean,
    val showHistory: Boolean,
) {
    @SerialName("daily")
    DAILY(true, true),

    @SerialName("all_time")
    ALL_TIME(false, false),
}
