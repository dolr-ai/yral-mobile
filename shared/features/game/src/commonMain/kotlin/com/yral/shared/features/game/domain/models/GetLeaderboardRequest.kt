package com.yral.shared.features.game.domain.models

import com.yral.shared.features.game.data.models.GetLeaderboardRequestDto
import com.yral.shared.features.game.data.models.LeaderboardMode

data class GetLeaderboardRequest(
    val principalId: String,
    val mode: LeaderboardMode,
)

fun GetLeaderboardRequest.toDto(): GetLeaderboardRequestDto =
    GetLeaderboardRequestDto(
        principalId = principalId,
        mode = mode,
    )
