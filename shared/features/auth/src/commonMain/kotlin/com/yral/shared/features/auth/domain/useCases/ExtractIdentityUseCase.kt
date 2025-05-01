package com.yral.shared.features.auth.domain.useCases

import com.yral.shared.core.SuspendUseCase
import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.features.auth.domain.AuthRepository
import io.ktor.http.Cookie

class ExtractIdentityUseCase(
    appDispatchers: AppDispatchers,
    private val authRepository: AuthRepository,
) : SuspendUseCase<Cookie, ByteArray>(appDispatchers.io) {
    override suspend fun execute(parameter: Cookie): ByteArray =
        authRepository
            .extractIdentity(parameter)
}
