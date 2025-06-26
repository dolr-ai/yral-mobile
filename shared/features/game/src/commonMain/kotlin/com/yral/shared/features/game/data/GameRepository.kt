package com.yral.shared.features.game.data

import com.yral.shared.features.game.data.models.toCastVoteResponse
import com.yral.shared.features.game.data.models.toGetBalanceResponse
import com.yral.shared.features.game.domain.IGameRepository
import com.yral.shared.features.game.domain.models.CastVoteRequest
import com.yral.shared.features.game.domain.models.CastVoteResponse
import com.yral.shared.features.game.domain.models.GetBalanceResponse
import com.yral.shared.features.game.domain.models.toDto

class GameRepository(
    private val gamRemoteDataSource: IGameRemoteDataSource,
) : IGameRepository {
    override suspend fun castVote(request: CastVoteRequest): CastVoteResponse =
        gamRemoteDataSource
            .castVote(request.idToken, request.toDto())
            .toCastVoteResponse()

    override suspend fun getBalance(userPrincipal: String): GetBalanceResponse =
        gamRemoteDataSource
            .getBalance(userPrincipal)
            .toGetBalanceResponse()
}
