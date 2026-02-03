package com.yral.shared.features.game.domain.models

import com.yral.shared.features.game.data.models.CastHotOrNotVoteRequestDto

data class CastHotOrNotVoteRequest(
    val idToken: String = "",
    val principalId: String,
    val videoId: String,
    val isHot: Boolean,
)

fun CastHotOrNotVoteRequest.toDto(): CastHotOrNotVoteRequestDto =
    CastHotOrNotVoteRequestDto(
        principalId = principalId,
        videoId = videoId,
        vote = if (isHot) "hot" else "not",
    )
