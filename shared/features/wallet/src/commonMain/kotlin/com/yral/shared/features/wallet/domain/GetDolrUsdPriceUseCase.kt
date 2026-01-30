package com.yral.shared.features.wallet.domain

import com.yral.shared.features.wallet.domain.models.DolrPrice
import com.yral.shared.features.wallet.domain.repository.WalletRepository
import com.yral.shared.libs.arch.domain.UnitSuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class GetDolrUsdPriceUseCase(
    private val repository: WalletRepository,
    appDispatchers: AppDispatchers,
    failureListener: UseCaseFailureListener,
) : UnitSuspendUseCase<DolrPrice>(appDispatchers.network, failureListener) {
    override suspend fun execute(parameter: Unit): DolrPrice = repository.getDolrPrice()
}
