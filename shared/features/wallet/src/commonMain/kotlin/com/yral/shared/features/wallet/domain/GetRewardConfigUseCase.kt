package com.yral.shared.features.wallet.domain

import com.yral.shared.features.wallet.domain.models.BtcRewardConfig
import com.yral.shared.features.wallet.domain.repository.WalletRepository
import com.yral.shared.libs.arch.domain.UnitSuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class GetRewardConfigUseCase(
    private val repository: WalletRepository,
    appDispatchers: AppDispatchers,
    failureListener: UseCaseFailureListener,
) : UnitSuspendUseCase<BtcRewardConfig>(appDispatchers.network, failureListener) {
    override suspend fun execute(parameter: Unit): BtcRewardConfig = repository.getBtcRewardConfig()
}
