package com.yral.shared.features.game.domain

import com.yral.shared.features.game.domain.models.CastVoteRequest
import com.yral.shared.features.game.domain.models.CastVoteResponse

interface IGameRepository {
    suspend fun castVote(request: CastVoteRequest): CastVoteResponse
}
