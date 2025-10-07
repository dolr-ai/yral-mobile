package com.yral.shared.features.leaderboard.domain.models

import com.yral.shared.features.leaderboard.data.models.GetLeaderboardRequestDto
import com.yral.shared.features.leaderboard.data.models.LeaderboardMode

data class GetLeaderboardRequest(
    val principalId: String,
    val mode: LeaderboardMode,
    val countryCode: String,
)

fun GetLeaderboardRequest.toDto(): GetLeaderboardRequestDto =
    GetLeaderboardRequestDto(
        principalId = principalId,
        mode = mode,
        countryCode = countryCode,
    )
