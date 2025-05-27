package com.yral.shared.features.game.data

import com.github.michaelbull.result.getOrThrow
import com.yral.shared.firebaseStore.model.AboutGameItemDto
import com.yral.shared.firebaseStore.model.GameConfigDto
import com.yral.shared.firebaseStore.usecase.GetCollectionUseCase
import com.yral.shared.firebaseStore.usecase.GetFBDocumentUseCase

class GameRemoteDataSource(
    private val getConfigUseCase: GetFBDocumentUseCase<GameConfigDto>,
    private val getAboutUseCase: GetCollectionUseCase<AboutGameItemDto>,
) : IGameRemoteDataSource {
    override suspend fun getConfig(): GameConfigDto =
        getConfigUseCase
            .invoke(
                parameter =
                    GetFBDocumentUseCase.Params(
                        collectionPath = GAME_CONFIG_COLLECTION,
                        documentId = GAME_CONFIG_DOCUMENT,
                    ),
            ).getOrThrow()

    override suspend fun getRules(): List<AboutGameItemDto> =
        getAboutUseCase
            .invoke(GAME_ABOUT_COLLECTION)
            .getOrThrow()

    companion object {
        private const val GAME_CONFIG_COLLECTION = "config"
        private const val GAME_CONFIG_DOCUMENT = "smiley_game_v1"
        private const val GAME_ABOUT_COLLECTION = "smiley_game_rules"
    }
}
