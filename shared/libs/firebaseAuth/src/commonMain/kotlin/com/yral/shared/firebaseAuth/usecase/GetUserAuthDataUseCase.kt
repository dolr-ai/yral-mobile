package com.yral.shared.firebaseAuth.usecase

import com.yral.shared.core.exceptions.YralException
import com.yral.shared.firebaseAuth.model.UserAuthData
import com.yral.shared.firebaseAuth.repository.FBAuthRepositoryApi
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class GetUserAuthDataUseCase(
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
    private val repository: FBAuthRepositoryApi,
) : SuspendUseCase<GetUserAuthDataUseCase.Parameters, UserAuthData>(
        coroutineDispatcher = appDispatchers.network,
        failureListener = useCaseFailureListener,
    ) {
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
