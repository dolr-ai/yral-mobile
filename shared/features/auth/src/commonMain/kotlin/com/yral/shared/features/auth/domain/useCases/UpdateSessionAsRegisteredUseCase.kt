package com.yral.shared.features.auth.domain.useCases

import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.features.auth.domain.AuthRepository
import com.yral.shared.libs.useCase.SuspendUseCase

class UpdateSessionAsRegisteredUseCase(
    appDispatchers: AppDispatchers,
    crashlyticsManager: CrashlyticsManager,
    private val authRepository: AuthRepository,
) : SuspendUseCase<UpdateSessionAsRegisteredUseCase.Params, Unit>(appDispatchers.io, crashlyticsManager) {
    override suspend fun execute(parameter: Params): Unit =
        authRepository
            .updateSessionAsRegistered(
                idToken = parameter.idToken,
                canisterId = parameter.canisterId,
            )

    data class Params(
        val idToken: String,
        val canisterId: String,
    )
}
