package com.yral.shared.firebaseAuth.usecase

import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.firebaseAuth.repository.FBAuthRepositoryApi
import com.yral.shared.libs.useCase.SuspendUseCase

class GetCurrentUserIdUseCase(
    appDispatchers: AppDispatchers,
    crashlyticsManager: CrashlyticsManager,
    private val repository: FBAuthRepositoryApi,
) : SuspendUseCase<Unit, String?>(appDispatchers.io, crashlyticsManager) {
    override suspend fun execute(parameter: Unit): String? = repository.getCurrentUserId()
}
