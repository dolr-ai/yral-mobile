package com.yral.shared.features.auth.domain.useCases

import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.features.auth.domain.AuthRepository
import com.yral.shared.features.auth.domain.models.PhoneAuthLoginResponse
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class PhoneAuthLoginUseCase(
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
    private val authRepository: AuthRepository,
) : SuspendUseCase<PhoneAuthLoginUseCase.Params, PhoneAuthLoginResponse>(
        appDispatchers.network,
        useCaseFailureListener,
    ) {
    override val exceptionType: String
        get() = ExceptionType.AUTH.name

    override suspend fun execute(parameter: Params): PhoneAuthLoginResponse =
        authRepository
            .phoneAuthLogin(
                phoneNumber = parameter.phoneNumber,
                identity = parameter.identity,
            )

    data class Params(
        val phoneNumber: String,
        val identity: ByteArray,
    )
}
