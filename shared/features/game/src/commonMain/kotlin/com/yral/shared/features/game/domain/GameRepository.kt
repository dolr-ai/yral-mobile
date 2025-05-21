package com.yral.shared.features.game.domain

import com.yral.shared.features.game.data.IGameRemoteDataSource
import com.yral.shared.features.game.data.models.toGameConfig
import com.yral.shared.features.game.domain.models.GameConfig

class GameRepository(
    private val gamRemoteDataSource: IGameRemoteDataSource,
) : IGameRepository {
    override suspend fun getConfig(): GameConfig =
        gamRemoteDataSource
            .getConfig()
            .toGameConfig()
}
