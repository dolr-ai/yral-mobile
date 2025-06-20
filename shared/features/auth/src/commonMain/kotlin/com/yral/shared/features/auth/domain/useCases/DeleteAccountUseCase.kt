package com.yral.shared.features.auth.domain.useCases

import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.features.auth.domain.AuthRepository
import com.yral.shared.libs.useCase.SuspendUseCase

class DeleteAccountUseCase(
    private val repository: AuthRepository,
    dispatchers: AppDispatchers,
    crashlyticsManager: CrashlyticsManager,
) : SuspendUseCase<Unit, String>(dispatchers.io, crashlyticsManager) {
    override suspend fun execute(parameter: Unit): String = repository.deleteAccount()
}
