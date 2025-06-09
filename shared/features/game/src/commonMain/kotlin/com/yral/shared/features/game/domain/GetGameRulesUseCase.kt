package com.yral.shared.features.game.domain

import com.github.michaelbull.result.getOrThrow
import com.yral.shared.core.AppConfigurations.FIREBASE_BUCKET
import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.features.game.data.models.toAboutGameItem
import com.yral.shared.features.game.domain.models.AboutGameItem
import com.yral.shared.firebaseStore.model.AboutGameItemDto
import com.yral.shared.firebaseStore.usecase.GetCollectionUseCase
import com.yral.shared.libs.useCase.SuspendUseCase
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.storage.storage

class GetGameRulesUseCase(
    appDispatchers: AppDispatchers,
    crashlyticsManager: CrashlyticsManager,
    private val getAboutUseCase: GetCollectionUseCase<AboutGameItemDto>,
) : SuspendUseCase<Unit, List<AboutGameItem>>(appDispatchers.io, crashlyticsManager) {
    override suspend fun execute(parameter: Unit): List<AboutGameItem> {
        val rules =
            getAboutUseCase
                .invoke(GAME_ABOUT_COLLECTION)
                .getOrThrow()
                .map { it.toAboutGameItem() }
        val storage = Firebase.storage(FIREBASE_BUCKET)
        val storageRef = storage.reference
        return rules.map { rule ->
            rule.copy(
                thumbnailUrl =
                    if (rule.thumbnailUrl.isNotEmpty()) {
                        storageRef.child(rule.thumbnailUrl).getDownloadUrl()
                    } else {
                        rule.thumbnailUrl
                    },
                body =
                    rule.body.map { body ->
                        body.copy(
                            imageUrls =
                                body.imageUrls?.map { imageUrl ->
                                    if (imageUrl.isNotEmpty()) {
                                        storageRef.child(imageUrl).getDownloadUrl()
                                    } else {
                                        imageUrl
                                    }
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
