package com.yral.shared.features.leaderboard.domain.models

import com.yral.shared.features.leaderboard.data.models.LeaderboardHistoryRequestDto

data class LeaderboardHistoryRequest(
    val principalId: String,
)

fun LeaderboardHistoryRequest.toDto(): LeaderboardHistoryRequestDto = LeaderboardHistoryRequestDto(principalId)
