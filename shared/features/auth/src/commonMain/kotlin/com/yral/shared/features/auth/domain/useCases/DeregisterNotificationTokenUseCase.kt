package com.yral.shared.features.auth.domain.useCases

import co.touchlab.kermit.Logger
import com.yral.shared.features.auth.domain.AuthRepository
import com.yral.shared.libs.arch.domain.UnitSuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.messaging.messaging

class DeregisterNotificationTokenUseCase(
    appDispatchers: AppDispatchers,
    failureListener: UseCaseFailureListener,
    private val authRepository: AuthRepository,
) : UnitSuspendUseCase<Unit>(appDispatchers.network, failureListener) {
    override suspend fun execute(parameter: Unit) {
        val token = Firebase.messaging.getToken()
        Logger.d("DeregisterNotificationTokenUseCase") { "DeRegistering token $token" }
        authRepository.deregisterForNotifications(token)
    }
}
