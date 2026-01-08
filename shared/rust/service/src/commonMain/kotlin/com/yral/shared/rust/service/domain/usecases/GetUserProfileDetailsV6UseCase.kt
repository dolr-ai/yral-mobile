package com.yral.shared.rust.service.domain.usecases

import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.rust.service.domain.models.UserProfileDetails

class GetUserProfileDetailsV6UseCase(
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
    private val userInfoRepository: com.yral.shared.rust.service.domain.UserInfoRepository,
) : SuspendUseCase<GetUserProfileDetailsV6Params, UserProfileDetails>(
        appDispatchers.network,
        useCaseFailureListener,
    ) {
    override suspend fun execute(parameter: GetUserProfileDetailsV6Params): UserProfileDetails =
        userInfoRepository.getUserProfileDetailsV6(
            principal = parameter.principal,
            targetPrincipal = parameter.targetPrincipal,
        )
}

data class GetUserProfileDetailsV6Params(
    val principal: String,
    val targetPrincipal: String,
)
