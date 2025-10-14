package com.yral.shared.rust.service.domain.usecases

import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.uniffi.generated.Principal

class FollowUserUseCase(
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
    private val userInfoRepository: com.yral.shared.rust.service.domain.UserInfoRepository,
) : SuspendUseCase<FollowUserParams, Unit>(appDispatchers.network, useCaseFailureListener) {
    override suspend fun execute(parameter: FollowUserParams) {
        userInfoRepository.followUser(
            principal = parameter.principal,
            targetPrincipal = parameter.targetPrincipal,
        )
    }
}

data class FollowUserParams(
    val principal: Principal,
    val targetPrincipal: Principal,
)
