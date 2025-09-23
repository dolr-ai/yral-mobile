package com.yral.shared.features.game.domain

import com.github.michaelbull.result.Result
import com.yral.shared.features.game.domain.models.AutoRechargeBalanceError
import com.yral.shared.features.game.domain.models.AutoRechargeBalanceRequest
import com.yral.shared.features.game.domain.models.CastVoteRequest
import com.yral.shared.features.game.domain.models.CastVoteResponse
import com.yral.shared.features.game.domain.models.GetBalanceResponse
import com.yral.shared.features.game.domain.models.UpdatedBalance

interface IGameRepository {
    suspend fun castVote(request: CastVoteRequest): CastVoteResponse
    suspend fun getBalance(userPrincipal: String): GetBalanceResponse

    suspend fun autoRechargeBalance(
        idToken: String,
        request: AutoRechargeBalanceRequest,
    ): Result<UpdatedBalance, AutoRechargeBalanceError>
}
