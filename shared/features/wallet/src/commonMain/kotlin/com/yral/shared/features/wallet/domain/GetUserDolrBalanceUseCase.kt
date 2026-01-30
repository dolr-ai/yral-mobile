package com.yral.shared.features.wallet.domain

import com.yral.shared.core.session.SessionManager
import com.yral.shared.features.wallet.domain.repository.WalletRepository
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class GetUserDolrBalanceUseCase(
    private val repository: WalletRepository,
    private val sessionManager: SessionManager,
    appDispatchers: AppDispatchers,
    failureListener: UseCaseFailureListener,
) : SuspendUseCase<String, Double>(appDispatchers.network, failureListener) {
    override suspend fun execute(parameter: String): Double =
        sessionManager.canisterID?.let { canisterId ->
            val rawBalance =
                repository
                    .getUserDolrBalance(
                        canisterId = canisterId,
                        userPrincipal = parameter,
                    ).toDoubleOrNull() ?: 0.0
            rawBalance / DOLR_DECIMAL_DIVISOR
        } ?: 0.0

    companion object {
        private const val DOLR_DECIMAL_DIVISOR = 100_000_000.0 // 10^8
    }
}
