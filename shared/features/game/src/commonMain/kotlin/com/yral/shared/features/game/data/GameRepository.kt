package com.yral.shared.features.game.data

import com.yral.shared.features.game.data.models.toAboutGameItem
import com.yral.shared.features.game.data.models.toCastVoteResponse
import com.yral.shared.features.game.data.models.toGameConfig
import com.yral.shared.features.game.domain.IGameRepository
import com.yral.shared.features.game.domain.models.AboutGameItem
import com.yral.shared.features.game.domain.models.CastVoteRequest
import com.yral.shared.features.game.domain.models.CastVoteResponse
import com.yral.shared.features.game.domain.models.GameConfig
import com.yral.shared.features.game.domain.models.toDto

class GameRepository(
    private val gamRemoteDataSource: IGameRemoteDataSource,
) : IGameRepository {
    override suspend fun getConfig(): GameConfig =
        gamRemoteDataSource
            .getConfig()
            .toGameConfig()

    override suspend fun getRules(): List<AboutGameItem> =
        gamRemoteDataSource
            .getRules()
            .map { it.toAboutGameItem() }

    override suspend fun castVote(request: CastVoteRequest): CastVoteResponse =
        gamRemoteDataSource
            .castVote(request.idToken, request.toDto())
            .toCastVoteResponse()
}
