package com.yral.shared.features.game.domain.models

import com.yral.shared.features.game.data.models.GetLeaderboardRequestDto

data class GetLeaderboardRequest(
    val principalId: String,
)

fun GetLeaderboardRequest.toDto(): GetLeaderboardRequestDto =
    GetLeaderboardRequestDto(
        principalId = principalId,
    )
