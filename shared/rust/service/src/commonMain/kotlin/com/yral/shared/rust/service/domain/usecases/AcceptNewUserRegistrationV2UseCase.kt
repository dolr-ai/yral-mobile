package com.yral.shared.rust.service.domain.usecases

import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.rust.service.domain.UserInfoRepository
import com.yral.shared.uniffi.generated.Principal

class AcceptNewUserRegistrationV2UseCase(
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
    private val userInfoRepository: UserInfoRepository,
) : SuspendUseCase<AcceptNewUserRegistrationV2Params, Unit>(
        appDispatchers.network,
        useCaseFailureListener,
    ) {
    override suspend fun execute(parameter: AcceptNewUserRegistrationV2Params) {
        userInfoRepository.acceptNewUserRegistrationV2(
            principal = parameter.principal,
            newPrincipal = parameter.newPrincipal,
            authenticated = parameter.authenticated,
            mainAccount = parameter.mainAccount,
        )
    }
}

data class AcceptNewUserRegistrationV2Params(
    val principal: Principal,
    val newPrincipal: Principal,
    val authenticated: Boolean,
    val mainAccount: Principal?,
)
