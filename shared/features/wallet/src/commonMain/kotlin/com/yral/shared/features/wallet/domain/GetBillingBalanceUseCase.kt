package com.yral.shared.features.wallet.domain

import com.yral.shared.features.wallet.domain.models.BillingBalance
import com.yral.shared.features.wallet.domain.repository.WalletRepository
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class GetBillingBalanceUseCase(
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
    private val walletRepository: WalletRepository,
) : SuspendUseCase<String, BillingBalance>(appDispatchers.network, useCaseFailureListener) {
    override suspend fun execute(parameter: String): BillingBalance = walletRepository.getBillingBalance(parameter)
}
