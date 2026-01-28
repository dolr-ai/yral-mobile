package com.yral.shared.rust.service.domain.usecases

import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.rust.service.domain.UserInfoRepository
import com.yral.shared.rust.service.domain.models.UserProfileDetails

class GetUserProfileDetailsV7UseCase(
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
    private val userInfoRepository: UserInfoRepository,
) : SuspendUseCase<GetUserProfileDetailsV7Params, UserProfileDetails>(
        appDispatchers.network,
        useCaseFailureListener,
    ) {
    override suspend fun execute(parameter: GetUserProfileDetailsV7Params): UserProfileDetails =
        userInfoRepository.getUserProfileDetailsV7(
            principal = parameter.principal,
            targetPrincipal = parameter.targetPrincipal,
        )
}

data class GetUserProfileDetailsV7Params(
    val principal: String,
    val targetPrincipal: String,
)
