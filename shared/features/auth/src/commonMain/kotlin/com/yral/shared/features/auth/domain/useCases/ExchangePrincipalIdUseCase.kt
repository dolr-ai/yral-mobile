package com.yral.shared.features.auth.domain.useCases

import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.features.auth.domain.AuthRepository
import com.yral.shared.features.auth.domain.models.ExchangePrincipalResponse
import com.yral.shared.libs.useCase.SuspendUseCase

class ExchangePrincipalIdUseCase(
    appDispatchers: AppDispatchers,
    crashlyticsManager: CrashlyticsManager,
    private val authRepository: AuthRepository,
) : SuspendUseCase<ExchangePrincipalIdUseCase.Params, ExchangePrincipalResponse>(
        appDispatchers.io,
        crashlyticsManager,
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
