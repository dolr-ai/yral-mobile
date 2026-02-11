package com.yral.shared.features.leaderboard.domain.models

import com.yral.shared.features.leaderboard.data.models.LeaderboardHistoryRequestDto

data class LeaderboardHistoryRequest(
    val principalId: String,
    val countryCode: String,
    val gameType: DailyRankGameType = DailyRankGameType.SMILEY,
)

fun LeaderboardHistoryRequest.toDto(): LeaderboardHistoryRequestDto =
    LeaderboardHistoryRequestDto(
        principalId = principalId,
        countryCode = countryCode,
        gameType = gameType.apiValue,
    )
