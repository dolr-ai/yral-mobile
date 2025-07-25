package com.yral.shared.features.auth.domain.useCases

import com.yral.shared.features.auth.domain.AuthRepository
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class DeregisterNotificationTokenUseCase(
    appDispatchers: AppDispatchers,
    failureListener: UseCaseFailureListener,
    private val authRepository: AuthRepository,
) : SuspendUseCase<DeregisterNotificationTokenUseCase.Parameter, Unit>(
        appDispatchers.network,
        failureListener,
    ) {
    override suspend fun execute(parameter: Parameter) {
        authRepository.deregisterForNotifications(parameter.token)
    }

    data class Parameter(
        val token: String,
    )
}
