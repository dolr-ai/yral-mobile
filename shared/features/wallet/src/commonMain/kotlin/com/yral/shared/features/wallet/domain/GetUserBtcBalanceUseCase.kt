package com.yral.shared.features.wallet.domain

import com.yral.shared.features.wallet.domain.repository.WalletRepository
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class GetUserBtcBalanceUseCase(
    private val repository: WalletRepository,
    appDispatchers: AppDispatchers,
    failureListener: UseCaseFailureListener,
) : SuspendUseCase<String, Double>(appDispatchers.network, failureListener) {
    override suspend fun execute(parameter: String): Double =
        repository
            .getUserBtcBalance(parameter)
            .toDouble()
}
