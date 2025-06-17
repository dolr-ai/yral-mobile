package com.yral.shared.features.game.domain

import com.yral.shared.features.game.domain.models.CastVoteRequest
import com.yral.shared.features.game.domain.models.CastVoteResponse
import com.yral.shared.features.game.domain.models.GetBalanceResponse

interface IGameRepository {
    suspend fun castVote(request: CastVoteRequest): CastVoteResponse
    suspend fun getBalance(userPrincipal: String): GetBalanceResponse
}
