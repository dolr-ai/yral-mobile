package com.yral.shared.features.wallet.domain

import com.yral.shared.features.wallet.domain.models.Transaction
import com.yral.shared.features.wallet.domain.repository.WalletRepository
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class GetTransactionsUseCase(
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
    private val walletRepository: WalletRepository,
) : SuspendUseCase<String, List<Transaction>>(appDispatchers.network, useCaseFailureListener) {
    override suspend fun execute(parameter: String): List<Transaction> = walletRepository.getTransactions(parameter)
}
