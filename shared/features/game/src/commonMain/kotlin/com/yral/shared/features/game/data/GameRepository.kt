package com.yral.shared.features.game.data

import com.github.michaelbull.result.Result
import com.yral.shared.features.game.data.models.toAutoRechargeBalanceResponse
import com.yral.shared.features.game.data.models.toCastVoteResponse
import com.yral.shared.features.game.data.models.toGetBalanceResponse
import com.yral.shared.features.game.data.models.toLeaderboardData
import com.yral.shared.features.game.data.models.toLeaderboardHistory
import com.yral.shared.features.game.domain.IGameRepository
import com.yral.shared.features.game.domain.models.AutoRechargeBalanceError
import com.yral.shared.features.game.domain.models.AutoRechargeBalanceRequest
import com.yral.shared.features.game.domain.models.CastVoteRequest
import com.yral.shared.features.game.domain.models.CastVoteResponse
import com.yral.shared.features.game.domain.models.GetBalanceResponse
import com.yral.shared.features.game.domain.models.GetLeaderboardRequest
import com.yral.shared.features.game.domain.models.LeaderboardData
import com.yral.shared.features.game.domain.models.LeaderboardError
import com.yral.shared.features.game.domain.models.LeaderboardHistory
import com.yral.shared.features.game.domain.models.LeaderboardHistoryError
import com.yral.shared.features.game.domain.models.LeaderboardHistoryRequest
import com.yral.shared.features.game.domain.models.UpdatedBalance
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

    override suspend fun autoRechargeBalance(
        idToken: String,
        request: AutoRechargeBalanceRequest,
    ): Result<UpdatedBalance, AutoRechargeBalanceError> =
        gamRemoteDataSource
            .autoRechargeBalance(idToken, request.toDto())
            .toAutoRechargeBalanceResponse()

    override suspend fun getLeaderboard(
        idToken: String,
        request: GetLeaderboardRequest,
    ): Result<LeaderboardData, LeaderboardError> =
        gamRemoteDataSource
            .getLeaderboard(idToken, request.toDto())
            .toLeaderboardData()

    override suspend fun getLeaderboardHistory(
        idToken: String,
        request: LeaderboardHistoryRequest,
    ): Result<LeaderboardHistory, LeaderboardHistoryError> =
        gamRemoteDataSource
            .getLeaderboardHistory(idToken, request.toDto())
            .toLeaderboardHistory()
}
