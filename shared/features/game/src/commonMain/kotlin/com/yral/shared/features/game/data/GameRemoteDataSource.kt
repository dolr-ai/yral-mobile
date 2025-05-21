package com.yral.shared.features.game.data

import com.yral.shared.features.game.data.models.GameConfigDto
import kotlinx.serialization.json.Json

class GameRemoteDataSource(
    private val json: Json,
) : IGameRemoteDataSource {
    override suspend fun getConfig(): GameConfigDto {
        val dummy =
            """""".trimIndent()
        return json.decodeFromString(dummy)
    }
}
