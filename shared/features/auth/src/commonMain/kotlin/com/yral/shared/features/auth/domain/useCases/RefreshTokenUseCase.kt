package com.yral.shared.features.auth.domain.useCases

import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.features.auth.domain.AuthRepository
import com.yral.shared.features.auth.domain.models.TokenResponse
import com.yral.shared.libs.useCase.SuspendUseCase

class RefreshTokenUseCase(
    appDispatchers: AppDispatchers,
    crashlyticsManager: CrashlyticsManager,
    private val authRepository: AuthRepository,
) : SuspendUseCase<String, TokenResponse>(appDispatchers.io, crashlyticsManager) {
    override suspend fun execute(parameter: String): TokenResponse =
        authRepository
            .refreshToken(parameter)
}
