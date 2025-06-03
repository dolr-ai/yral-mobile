package com.yral.shared.features.game.domain

import com.yral.shared.features.game.domain.models.AboutGameItem
import com.yral.shared.features.game.domain.models.CastVoteRequest
import com.yral.shared.features.game.domain.models.CastVoteResponse
import com.yral.shared.features.game.domain.models.GameConfig

interface IGameRepository {
    suspend fun getConfig(): GameConfig
    suspend fun getRules(): List<AboutGameItem>
    suspend fun castVote(request: CastVoteRequest): CastVoteResponse
}
