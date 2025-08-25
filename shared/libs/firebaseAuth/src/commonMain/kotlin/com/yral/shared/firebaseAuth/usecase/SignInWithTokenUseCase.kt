package com.yral.shared.firebaseAuth.usecase

import com.yral.shared.firebaseAuth.repository.FBAuthRepositoryApi
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class SignInWithTokenUseCase(
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
    private val repository: FBAuthRepositoryApi,
) : SuspendUseCase<String, String>(
        coroutineDispatcher = appDispatchers.network,
        failureListener = useCaseFailureListener,
    ) {
    override suspend fun execute(parameter: String): String =
        repository
            .signInWithToken(token = parameter)
            .getOrThrow()
}
