package com.yral.shared.features.wallet.domain

import com.github.michaelbull.result.getOrThrow
import com.yral.shared.features.wallet.domain.models.BtcInInr
import com.yral.shared.features.wallet.domain.repository.WalletRepository
import com.yral.shared.firebaseAuth.usecase.GetIdTokenUseCase
import com.yral.shared.libs.arch.domain.UnitSuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class GetBtcInInrUseCase(
    private val repository: WalletRepository,
    private val getIdTokenUseCase: GetIdTokenUseCase,
    appDispatchers: AppDispatchers,
    failureListener: UseCaseFailureListener,
) : UnitSuspendUseCase<BtcInInr>(appDispatchers.network, failureListener) {
    override suspend fun execute(parameter: Unit): BtcInInr {
        val idToken = getIdTokenUseCase.invoke(GetIdTokenUseCase.DEFAULT).getOrThrow()
        return repository.getBtcInInr(idToken)
    }
}
