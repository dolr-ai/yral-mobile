package com.yral.shared.features.game.domain

import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class GetBalanceUseCase(
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
    private val gameRepository: IGameRepository,
) : SuspendUseCase<String, Long>(appDispatchers.network, useCaseFailureListener) {
    override suspend fun execute(parameter: String): Long =
        gameRepository
            .getBalance(parameter)
            .balance
}
