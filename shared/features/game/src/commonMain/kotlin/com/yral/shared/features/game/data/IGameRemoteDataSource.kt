package com.yral.shared.features.game.data

import com.yral.shared.features.game.data.models.AutoRechargeBalanceRequestDto
import com.yral.shared.features.game.data.models.AutoRechargeBalanceResponseDto
import com.yral.shared.features.game.data.models.CastVoteRequestDto
import com.yral.shared.features.game.data.models.CastVoteResponseDto
import com.yral.shared.features.game.data.models.GetBalanceResponseDto

interface IGameRemoteDataSource {
    suspend fun castVote(
        idToken: String,
        request: CastVoteRequestDto,
    ): CastVoteResponseDto
    suspend fun getBalance(userPrincipal: String): GetBalanceResponseDto
    suspend fun autoRechargeBalance(
        idToken: String,
        request: AutoRechargeBalanceRequestDto,
    ): AutoRechargeBalanceResponseDto
}
