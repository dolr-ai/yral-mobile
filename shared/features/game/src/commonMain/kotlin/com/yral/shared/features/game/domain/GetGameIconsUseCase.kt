package com.yral.shared.features.game.domain

import com.github.michaelbull.result.getOrThrow
import com.yral.shared.features.game.data.models.toGameConfig
import com.yral.shared.features.game.domain.models.GameConfig
import com.yral.shared.firebaseStore.getDownloadUrl
import com.yral.shared.firebaseStore.model.GameConfigDto
import com.yral.shared.firebaseStore.usecase.GetFBDocumentUseCase
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import dev.gitlive.firebase.storage.FirebaseStorage

class GetGameIconsUseCase(
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
    private val getConfigUseCase: GetFBDocumentUseCase<GameConfigDto>,
    private val firebaseStorage: FirebaseStorage,
) : SuspendUseCase<Unit, GameConfig>(appDispatchers.network, useCaseFailureListener) {
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
                            unicode = unescapeUnicode(smiley.unicode),
                        )
                    },
        )
    }

    @Suppress("MagicNumber")
    private fun unescapeUnicode(input: String): String =
        Regex("""\\u([0-9A-Fa-f]{4})""")
            .replace(input) { matchResult ->
                val hexValue = matchResult.groupValues[1]
                val intValue = hexValue.toInt(16)
                intValue.toChar().toString()
            }

    companion object {
        private const val GAME_CONFIG_COLLECTION = "config"
        private const val GAME_CONFIG_DOCUMENT = "smiley_game_v2"
    }
}
