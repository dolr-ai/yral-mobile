package com.yral.shared.features.auth.domain.useCases

import com.yral.shared.features.auth.domain.AuthRepository
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class DeleteAccountUseCase(
    private val repository: AuthRepository,
    dispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
) : SuspendUseCase<Unit, String>(dispatchers.network, useCaseFailureListener) {
    override suspend fun execute(parameter: Unit): String = repository.deleteAccount()
}
