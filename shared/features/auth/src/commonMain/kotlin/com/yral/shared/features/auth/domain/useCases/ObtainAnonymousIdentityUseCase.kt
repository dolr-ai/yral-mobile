package com.yral.shared.features.auth.domain.useCases

import com.yral.shared.features.auth.domain.AuthRepository
import com.yral.shared.features.auth.domain.models.TokenResponse
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class ObtainAnonymousIdentityUseCase(
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
    private val authRepository: AuthRepository,
) : SuspendUseCase<Unit, TokenResponse>(appDispatchers.network, useCaseFailureListener) {
    override suspend fun execute(parameter: Unit): TokenResponse =
        authRepository
            .obtainAnonymousIdentity()
}
