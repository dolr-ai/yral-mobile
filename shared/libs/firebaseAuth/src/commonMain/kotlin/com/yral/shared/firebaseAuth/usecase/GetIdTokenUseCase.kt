package com.yral.shared.firebaseAuth.usecase

import com.yral.shared.core.exceptions.YralException
import com.yral.shared.firebaseAuth.repository.FBAuthRepositoryApi
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class GetIdTokenUseCase(
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
    private val repository: FBAuthRepositoryApi,
) : SuspendUseCase<GetIdTokenUseCase.Parameters, String>(
        coroutineDispatcher = appDispatchers.network,
        failureListener = useCaseFailureListener,
    ) {
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
