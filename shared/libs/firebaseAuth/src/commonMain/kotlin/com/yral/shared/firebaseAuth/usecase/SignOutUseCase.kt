package com.yral.shared.firebaseAuth.usecase

import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.firebaseAuth.repository.FBAuthRepositoryApi
import com.yral.shared.libs.useCase.SuspendUseCase

class SignOutUseCase(
    appDispatchers: AppDispatchers,
    crashlyticsManager: CrashlyticsManager,
    private val analyticsManager: AnalyticsManager,
    private val repository: FBAuthRepositoryApi,
) : SuspendUseCase<Unit, Unit>(appDispatchers.io, crashlyticsManager) {
    override suspend fun execute(parameter: Unit) =
        repository
            .signOut()
            .also { analyticsManager.reset() }
}
