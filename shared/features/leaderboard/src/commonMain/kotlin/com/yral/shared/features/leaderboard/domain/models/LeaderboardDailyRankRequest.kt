package com.yral.shared.features.leaderboard.domain.models

import com.yral.shared.features.leaderboard.data.models.LeaderboardDailyRankRequestDto

enum class DailyRankGameType(
    val apiValue: String,
) {
    SMILEY("smiley"),
    HOT_OR_NOT("hot_or_not"),
}

data class LeaderboardDailyRankRequest(
    val principalId: String,
    val gameType: DailyRankGameType = DailyRankGameType.SMILEY,
)

fun LeaderboardDailyRankRequest.toDto(): LeaderboardDailyRankRequestDto =
    LeaderboardDailyRankRequestDto(
        principalId = principalId,
        gameType = gameType.apiValue,
    )
