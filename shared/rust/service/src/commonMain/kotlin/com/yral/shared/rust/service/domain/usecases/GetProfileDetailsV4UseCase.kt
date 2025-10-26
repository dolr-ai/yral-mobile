package com.yral.shared.rust.service.domain.usecases

import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.rust.service.domain.models.UserProfileDetails

class GetProfileDetailsV4UseCase(
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
    private val userInfoRepository: com.yral.shared.rust.service.domain.UserInfoRepository,
) : SuspendUseCase<GetProfileDetailsV4Params, UserProfileDetails>(
        appDispatchers.network,
        useCaseFailureListener,
    ) {
    override suspend fun execute(parameter: GetProfileDetailsV4Params): UserProfileDetails =
        userInfoRepository.getProfileDetailsV4(
            principal = parameter.principal,
            targetPrincipal = parameter.targetPrincipal,
        )
}

data class GetProfileDetailsV4Params(
    val principal: String,
    val targetPrincipal: String,
)
