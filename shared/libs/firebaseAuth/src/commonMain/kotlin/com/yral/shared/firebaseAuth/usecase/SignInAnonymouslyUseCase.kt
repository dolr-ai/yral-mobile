package com.yral.shared.firebaseAuth.usecase

import com.yral.shared.firebaseAuth.repository.FBAuthRepositoryApi
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class SignInAnonymouslyUseCase(
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
    private val repository: FBAuthRepositoryApi,
) : SuspendUseCase<Unit, String>(
        coroutineDispatcher = appDispatchers.network,
        failureListener = useCaseFailureListener,
    ) {
    override suspend fun execute(parameter: Unit): String =
        repository
            .signInAnonymously()
            .getOrThrow()
}
