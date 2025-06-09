package com.yral.shared.features.game.data

import com.yral.shared.features.game.data.models.CastVoteRequestDto
import com.yral.shared.features.game.data.models.CastVoteResponseDto

interface IGameRemoteDataSource {
    suspend fun castVote(
        idToken: String,
        request: CastVoteRequestDto,
    ): CastVoteResponseDto
}
