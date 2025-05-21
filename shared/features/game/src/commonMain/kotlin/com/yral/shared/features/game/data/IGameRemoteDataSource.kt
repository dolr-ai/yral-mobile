package com.yral.shared.features.game.data

import com.yral.shared.features.game.data.models.GameConfigDto

interface IGameRemoteDataSource {
    suspend fun getConfig(): GameConfigDto
}
