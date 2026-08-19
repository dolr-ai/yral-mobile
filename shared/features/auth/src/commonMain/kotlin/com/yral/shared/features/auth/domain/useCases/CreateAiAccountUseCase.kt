package com.yral.shared.features.auth.domain.useCases

import com.yral.shared.features.auth.domain.AuthRepository
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class CreateAiAccountUseCase(
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
    private val authRepository: AuthRepository,
) : SuspendUseCase<CreateAiAccountUseCase.Params, String>(
        coroutineDispatcher = appDispatchers.network,
        failureListener = useCaseFailureListener,
    ) {
    override suspend fun execute(parameter: Params): String = authRepository.createAiAccount(userId = parameter.userId)

    data class Params(
        val userId: String,
    )
}
