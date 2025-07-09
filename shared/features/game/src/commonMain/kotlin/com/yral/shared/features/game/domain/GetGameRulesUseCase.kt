package com.yral.shared.features.game.domain

import com.github.michaelbull.result.getOrThrow
import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.features.game.data.models.toAboutGameItem
import com.yral.shared.features.game.domain.models.AboutGameItem
import com.yral.shared.firebaseStore.getDownloadUrl
import com.yral.shared.firebaseStore.model.AboutGameItemDto
import com.yral.shared.firebaseStore.usecase.GetCollectionUseCase
import com.yral.shared.libs.useCase.SuspendUseCase
import dev.gitlive.firebase.storage.FirebaseStorage

class GetGameRulesUseCase(
    appDispatchers: AppDispatchers,
    crashlyticsManager: CrashlyticsManager,
    private val getAboutUseCase: GetCollectionUseCase<AboutGameItemDto>,
    private val firebaseStorage: FirebaseStorage,
) : SuspendUseCase<Unit, List<AboutGameItem>>(appDispatchers.io, crashlyticsManager) {
    override suspend fun execute(parameter: Unit): List<AboutGameItem> {
        val rules =
            getAboutUseCase
                .invoke(
                    parameter =
                        GetCollectionUseCase.Params(
                            collectionName = GAME_ABOUT_COLLECTION,
                        ),
                ).getOrThrow()
                .map { it.toAboutGameItem() }
        return rules.map { rule ->
            rule.copy(
                thumbnailUrl = getDownloadUrl(rule.thumbnailUrl, firebaseStorage),
                body =
                    rule.body.map { body ->
                        body.copy(
                            imageUrls =
                                body.imageUrls?.map { imageUrl ->
                                    getDownloadUrl(imageUrl, firebaseStorage)
                                },
                        )
                    },
            )
        }
    }

    companion object {
        private const val GAME_ABOUT_COLLECTION = "smiley_game_rules"
    }
}
