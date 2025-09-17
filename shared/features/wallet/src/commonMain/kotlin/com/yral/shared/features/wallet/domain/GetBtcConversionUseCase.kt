package com.yral.shared.features.wallet.domain

import com.github.michaelbull.result.getOrThrow
import com.yral.shared.features.wallet.domain.models.BtcToCurrency
import com.yral.shared.features.wallet.domain.repository.WalletRepository
import com.yral.shared.firebaseAuth.usecase.GetIdTokenUseCase
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class GetBtcConversionUseCase(
    private val repository: WalletRepository,
    private val getIdTokenUseCase: GetIdTokenUseCase,
    appDispatchers: AppDispatchers,
    failureListener: UseCaseFailureListener,
) : SuspendUseCase<GetBtcConversionUseCase.Params, BtcToCurrency>(appDispatchers.network, failureListener) {
    override suspend fun execute(parameter: Params): BtcToCurrency {
        val idToken = getIdTokenUseCase.invoke(GetIdTokenUseCase.DEFAULT).getOrThrow()
        return repository.getBtcConversionRate(idToken, parameter.countryCode)
    }

    data class Params(
        val countryCode: String,
    )
}
