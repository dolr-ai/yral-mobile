package com.yral.shared.features.auth.domain.useCases

import co.touchlab.kermit.Logger
import com.yral.shared.features.auth.domain.AuthRepository
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class RegisterNotificationTokenUseCase(
    appDispatchers: AppDispatchers,
    failureListener: UseCaseFailureListener,
    private val authRepository: AuthRepository,
) : SuspendUseCase<RegisterNotificationTokenUseCase.Parameter, Unit>(
        appDispatchers.network,
        failureListener,
    ) {
    override suspend fun execute(parameter: Parameter) {
        Logger.d("RegisterNotificationTokenUseCase") { "Registering token ${parameter.token}" }
        authRepository.registerForNotifications(parameter.token)
    }

    data class Parameter(
        val token: String,
    )
}
