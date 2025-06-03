package com.yral.shared.firebaseAuth.usecase

import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.firebaseAuth.model.UserAuthData
import com.yral.shared.firebaseAuth.repository.FBAuthRepositoryApi
import com.yral.shared.libs.useCase.SuspendUseCase

class GetUserAuthDataUseCase(
    appDispatchers: AppDispatchers,
    crashlyticsManager: CrashlyticsManager,
    private val repository: FBAuthRepositoryApi,
) : SuspendUseCase<GetUserAuthDataUseCase.Parameters, UserAuthData>(appDispatchers.io, crashlyticsManager) {
    override suspend fun execute(parameter: Parameters): UserAuthData {
        val userId =
            repository
                .getCurrentUserId() ?: throw YralException("No user is currently signed in")

        val token =
            if (parameter.forceRefresh) {
                repository.refreshIdToken().getOrThrow()
            } else {
                repository.getIdToken()
            } ?: throw YralException("No token available for current user")

        return UserAuthData(
            userId = userId,
            idToken = token,
        )
    }

    data class Parameters(
        val forceRefresh: Boolean = false,
    )

    companion object {
        val DEFAULT = Parameters()
    }
}
