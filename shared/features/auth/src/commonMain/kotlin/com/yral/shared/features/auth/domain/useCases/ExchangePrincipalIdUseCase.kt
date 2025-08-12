package com.yral.shared.features.auth.domain.useCases

import com.yral.shared.features.auth.domain.AuthRepository
import com.yral.shared.features.auth.domain.models.ExchangePrincipalResponse
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class ExchangePrincipalIdUseCase(
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
    private val authRepository: AuthRepository,
) : SuspendUseCase<ExchangePrincipalIdUseCase.Params, ExchangePrincipalResponse>(
        coroutineDispatcher = appDispatchers.network,
        failureListener = useCaseFailureListener,
    ) {
    override suspend fun execute(parameter: Params): ExchangePrincipalResponse =
        authRepository.exchangePrincipalId(
            idToken = parameter.idToken,
            principalId = parameter.userPrincipal,
        )

    data class Params(
        val idToken: String,
        val userPrincipal: String,
    )
}
