package com.yral.shared.firebaseAuth.usecase

import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.firebaseAuth.repository.FBAuthRepositoryApi
import com.yral.shared.libs.useCase.SuspendUseCase

class GetIdTokenUseCase(
    appDispatchers: AppDispatchers,
    crashlyticsManager: CrashlyticsManager,
    private val repository: FBAuthRepositoryApi,
) : SuspendUseCase<GetIdTokenUseCase.Parameters, String>(appDispatchers.io, crashlyticsManager) {
    override suspend fun execute(parameter: Parameters): String =
        if (parameter.forceRefresh) {
            repository.refreshIdToken().getOrThrow()
        } else {
            repository.getIdToken()
        } ?: throw YralException("No token available for current user")

    data class Parameters(
        val forceRefresh: Boolean = false,
    )

    companion object {
        val DEFAULT = Parameters()
    }
}
