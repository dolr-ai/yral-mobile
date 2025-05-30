package com.yral.shared.features.game.domain.models

import com.yral.shared.features.game.data.models.CastVoteRequestDto

data class CastVoteRequest(
    val principalId: String,
    val videoId: String,
    val gameIconId: String,
)

fun CastVoteRequest.toDto(): CastVoteRequestDto =
    CastVoteRequestDto(
        principalId = principalId,
        videoId = videoId,
        gameIconId = gameIconId,
    )
