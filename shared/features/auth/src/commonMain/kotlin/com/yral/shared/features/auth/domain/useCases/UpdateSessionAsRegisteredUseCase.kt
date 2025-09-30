package com.yral.shared.features.auth.domain.useCases

import com.yral.shared.features.auth.domain.AuthRepository
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class UpdateSessionAsRegisteredUseCase(
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
    private val authRepository: AuthRepository,
) : SuspendUseCase<UpdateSessionAsRegisteredUseCase.Params, Unit>(appDispatchers.network, useCaseFailureListener) {
    override suspend fun execute(parameter: Params): Unit =
        authRepository
            .updateSessionAsRegistered(
                idToken = parameter.idToken,
                canisterId = parameter.canisterId,
                userPrincipal = parameter.userPrincipal,
            )

    data class Params(
        val idToken: String,
        val canisterId: String,
        val userPrincipal: String,
    )
}
