package com.yral.shared.features.auth.domain.useCases

import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.features.auth.domain.AuthRepository
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class PhoneAuthLoginUseCase(
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
    private val authRepository: AuthRepository,
) : SuspendUseCase<String, String>(appDispatchers.network, useCaseFailureListener) {
    override val exceptionType: String
        get() = ExceptionType.AUTH.name

    override suspend fun execute(parameter: String): String =
        authRepository
            .phoneAuthLogin(parameter)
}
