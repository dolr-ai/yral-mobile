package com.yral.shared.features.game.domain

import com.yral.shared.core.AppConfigurations.FIREBASE_BUCKET
import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.features.game.domain.models.GameIcon
import com.yral.shared.libs.useCase.SuspendUseCase
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.storage.storage

class GetGameIconsUseCase(
    appDispatchers: AppDispatchers,
    crashlyticsManager: CrashlyticsManager,
    private val gameRepository: IGameRepository,
) : SuspendUseCase<GetGameIconsUseCase.GetGameIconsParams, List<GameIcon>>(appDispatchers.io, crashlyticsManager) {
    override suspend fun execute(parameter: GetGameIconsParams): List<GameIcon> {
        val config = gameRepository.getConfig()
        if (config.lossPenalty < parameter.coinBalance) {
            val storage = Firebase.storage(FIREBASE_BUCKET)
            val storageRef = storage.reference
            return config.availableSmileys.map { smiley ->
                smiley.copy(
                    imageUrl =
                        if (smiley.imageUrl.isNotEmpty()) {
                            storageRef.child(smiley.imageUrl).getDownloadUrl()
                        } else {
                            smiley.imageUrl
                        },
                    clickAnimation =
                        if (smiley.clickAnimation.isNotEmpty()) {
                            storageRef.child(smiley.clickAnimation).getDownloadUrl()
                        } else {
                            smiley.clickAnimation
                        },
                )
            }
        }
        return emptyList()
    }

    data class GetGameIconsParams(
        val coinBalance: Long,
    )
}
