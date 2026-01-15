package com.yral.shared.features.auth.domain.useCases

import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.features.auth.domain.AuthRepository
import com.yral.shared.features.auth.domain.models.PhoneAuthVerifyResponse
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class VerifyPhoneAuthUseCase(
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
    private val authRepository: AuthRepository,
) : SuspendUseCase<VerifyPhoneAuthUseCase.Params, PhoneAuthVerifyResponse>(
        coroutineDispatcher = appDispatchers.network,
        failureListener = useCaseFailureListener,
    ) {
    override val exceptionType: String
        get() = ExceptionType.AUTH.name

    override suspend fun execute(parameter: Params): PhoneAuthVerifyResponse =
        authRepository.verifyPhoneAuth(
            phoneNumber = parameter.phoneNumber,
            code = parameter.code,
            clientState = parameter.clientState,
        )

    data class Params(
        val phoneNumber: String,
        val code: String,
        val clientState: String,
    )
}
