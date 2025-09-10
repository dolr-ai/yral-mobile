package com.yral.shared.features.game.domain.models

import com.yral.shared.features.game.data.models.LeaderboardHistoryRequestDto

data class LeaderboardHistoryRequest(
    val principalId: String,
)

fun LeaderboardHistoryRequest.toDto(): LeaderboardHistoryRequestDto = LeaderboardHistoryRequestDto(principalId)
