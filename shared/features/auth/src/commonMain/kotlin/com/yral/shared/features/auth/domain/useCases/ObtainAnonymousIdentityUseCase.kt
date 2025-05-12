package com.yral.shared.features.auth.domain.useCases

import com.yral.shared.core.SuspendUseCase
import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.features.auth.domain.AuthRepository
import com.yral.shared.features.auth.domain.models.TokenResponse

class ObtainAnonymousIdentityUseCase(
    appDispatchers: AppDispatchers,
    private val authRepository: AuthRepository,
) : SuspendUseCase<Unit, TokenResponse>(appDispatchers.io) {
    override suspend fun execute(parameter: Unit): TokenResponse =
        authRepository
            .obtainAnonymousIdentity()
}
