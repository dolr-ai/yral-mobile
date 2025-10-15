package com.yral.shared.features.leaderboard.domain.models

import com.yral.shared.features.leaderboard.data.models.LeaderboardDailyRankRequestDto

data class LeaderboardDailyRankRequest(
    val principalId: String,
)

fun LeaderboardDailyRankRequest.toDto(): LeaderboardDailyRankRequestDto =
    LeaderboardDailyRankRequestDto(
        principalId = principalId,
    )
