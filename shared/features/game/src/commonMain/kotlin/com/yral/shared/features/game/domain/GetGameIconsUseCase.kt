package com.yral.shared.features.game.domain

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
            return config.availableSmileys.map {
                if (it.imageUrl.isNotEmpty()) {
                    it.copy(
                        imageUrl = storageRef.child(it.imageUrl).getDownloadUrl(),
                    )
                } else {
                    it
                }
            }
        }
        return emptyList()
    }

    data class GetGameIconsParams(
        val coinBalance: Long,
    )

    companion object {
        private const val FIREBASE_BUCKET = "gs://yral-staging.firebasestorage.app"
    }
}
