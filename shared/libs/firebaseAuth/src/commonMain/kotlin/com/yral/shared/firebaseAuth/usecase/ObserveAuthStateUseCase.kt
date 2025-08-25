package com.yral.shared.firebaseAuth.usecase

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.yral.shared.firebaseAuth.model.AuthState
import com.yral.shared.firebaseAuth.repository.FBAuthRepositoryApi
import com.yral.shared.libs.arch.domain.FlowUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveAuthStateUseCase(
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
    private val repository: FBAuthRepositoryApi,
) : FlowUseCase<Unit, AuthState>(
        coroutineDispatcher = appDispatchers.disk,
        failureListener = useCaseFailureListener,
    ) {
    // Since the StateFlow from the repository can't fail (it always has a value)
    // we're wrapping each state in a successful Result using Ok
    override fun execute(parameters: Unit): Flow<Result<AuthState, Throwable>> =
        repository
            .observeAuthState()
            .map { Ok(it) }
}
