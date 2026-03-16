package com.yral.shared.features.wallet.domain

import com.yral.shared.features.wallet.domain.repository.WalletRepository
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class GetBalanceUseCase(
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
    private val walletRepository: WalletRepository,
) : SuspendUseCase<String, Long>(appDispatchers.network, useCaseFailureListener) {
    override suspend fun execute(parameter: String): Long =
        walletRepository
            .getBalance(parameter)
            .balance
}
