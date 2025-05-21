package com.yral.shared.features.game.domain

import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.features.game.domain.models.GameIcon
import com.yral.shared.libs.useCase.SuspendUseCase

class GetGameIconsUseCase(
    appDispatchers: AppDispatchers,
    crashlyticsManager: CrashlyticsManager,
    private val gameRepository: IGameRepository,
) : SuspendUseCase<GetGameIconsUseCase.GetGameIconsParams, List<GameIcon>>(appDispatchers.io, crashlyticsManager) {
    override suspend fun execute(parameter: GetGameIconsParams): List<GameIcon> {
        val config = gameRepository.getConfig()
        if (config.lossPenalty < parameter.coinBalance) {
            return config.availableSmileys
        }
        return emptyList()
    }

    data class GetGameIconsParams(
        val coinBalance: Long,
    )
}
