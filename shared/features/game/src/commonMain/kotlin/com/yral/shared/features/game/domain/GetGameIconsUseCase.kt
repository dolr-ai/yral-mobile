package com.yral.shared.features.game.domain

import co.touchlab.kermit.Logger
import com.github.michaelbull.result.getOrThrow
import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.features.game.data.models.toGameConfig
import com.yral.shared.features.game.domain.models.GameIcon
import com.yral.shared.firebaseStore.model.GameConfigDto
import com.yral.shared.firebaseStore.usecase.GetFBDocumentUseCase
import com.yral.shared.libs.useCase.SuspendUseCase
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.storage.storage

class GetGameIconsUseCase(
    appDispatchers: AppDispatchers,
    crashlyticsManager: CrashlyticsManager,
    private val getConfigUseCase: GetFBDocumentUseCase<GameConfigDto>,
) : SuspendUseCase<GetGameIconsUseCase.GetGameIconsParams, List<GameIcon>>(
        appDispatchers.io,
        crashlyticsManager,
    ) {
    override suspend fun execute(parameter: GetGameIconsParams): List<GameIcon> {
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
        if (config.lossPenalty < parameter.coinBalance) {
            return config.availableSmileys.map { smiley ->
                Logger.d("xxxx $smiley")
                smiley.copy(
                    imageUrl = getDownloadUrl(smiley.imageUrl),
                    clickAnimation = getDownloadUrl(smiley.clickAnimation),
                )
            }
        }
        return emptyList()
    }

    private suspend fun getDownloadUrl(path: String): String =
        if (path.isEmpty()) {
            path
        } else {
            runCatching {
                Firebase.storage.reference(path).getDownloadUrl().also {
                    Logger.d("xxxx download url: $it")
                }
            }.getOrElse { "" }
        }

    data class GetGameIconsParams(
        val coinBalance: Long,
    )

    companion object {
        private const val GAME_CONFIG_COLLECTION = "config"
        private const val GAME_CONFIG_DOCUMENT = "smiley_game_v1"
    }
}
