package com.yral.shared.features.game.domain

import com.github.michaelbull.result.getOrThrow
import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.features.game.data.models.toGameConfig
import com.yral.shared.features.game.domain.models.GameConfig
import com.yral.shared.firebaseStore.getDownloadUrl
import com.yral.shared.firebaseStore.model.GameConfigDto
import com.yral.shared.firebaseStore.usecase.GetFBDocumentUseCase
import com.yral.shared.libs.useCase.SuspendUseCase
import dev.gitlive.firebase.storage.FirebaseStorage

class GetGameIconsUseCase(
    appDispatchers: AppDispatchers,
    crashlyticsManager: CrashlyticsManager,
    private val getConfigUseCase: GetFBDocumentUseCase<GameConfigDto>,
    private val firebaseStorage: FirebaseStorage,
) : SuspendUseCase<Unit, GameConfig>(
        appDispatchers.io,
        crashlyticsManager,
    ) {
    override suspend fun execute(parameter: Unit): GameConfig {
        val config =
            getConfigUseCase
                .invoke(
                    parameter =
                        GetFBDocumentUseCase.Params(
                            collectionPath = GAME_CONFIG_COLLECTION,
                            documentId = GAME_CONFIG_DOCUMENT,
                        ),
                ).getOrThrow()
                .toGameConfig()

        return config.copy(
            availableSmileys =
                config.availableSmileys
                    .map { smiley ->
                        smiley.copy(
                            imageUrl = getDownloadUrl(smiley.imageUrl, firebaseStorage),
                            clickAnimation = getDownloadUrl(smiley.clickAnimation, firebaseStorage),
                        )
                    },
        )
    }

    companion object {
        private const val GAME_CONFIG_COLLECTION = "config"
        private const val GAME_CONFIG_DOCUMENT = "smiley_game_v1"
    }
}
