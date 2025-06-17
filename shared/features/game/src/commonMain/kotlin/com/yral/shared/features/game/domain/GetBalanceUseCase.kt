package com.yral.shared.features.game.domain

import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.libs.useCase.SuspendUseCase

class GetBalanceUseCase(
    appDispatchers: AppDispatchers,
    crashlyticsManager: CrashlyticsManager,
    private val gameRepository: IGameRepository,
) : SuspendUseCase<String, Long>(appDispatchers.io, crashlyticsManager) {
    override suspend fun execute(parameter: String): Long =
        gameRepository
            .getBalance(parameter)
            .balance
}
