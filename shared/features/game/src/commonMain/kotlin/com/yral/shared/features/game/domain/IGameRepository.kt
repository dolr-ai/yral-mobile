package com.yral.shared.features.game.domain

import com.yral.shared.features.game.domain.models.AboutGameItem
import com.yral.shared.features.game.domain.models.GameConfig

interface IGameRepository {
    suspend fun getConfig(): GameConfig
    suspend fun getRules(): List<AboutGameItem>
}
