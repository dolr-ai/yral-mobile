package com.yral.shared.features.game.data

import com.yral.shared.firebaseStore.model.AboutGameItemDto
import com.yral.shared.firebaseStore.model.GameConfigDto

interface IGameRemoteDataSource {
    suspend fun getConfig(): GameConfigDto
    suspend fun getRules(): List<AboutGameItemDto>
}
