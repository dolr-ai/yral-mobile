package com.yral.shared.rust.service.domain.usecases

import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class UnfollowUserUseCase(
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
    private val userInfoRepository: com.yral.shared.rust.service.domain.UserInfoRepository,
) : SuspendUseCase<UnfollowUserParams, Unit>(appDispatchers.network, useCaseFailureListener) {
    override suspend fun execute(parameter: UnfollowUserParams) {
        userInfoRepository.unfollowUser(
            principal = parameter.principal,
            targetPrincipal = parameter.targetPrincipal,
        )
    }
}

data class UnfollowUserParams(
    val principal: String,
    val targetPrincipal: String,
)
