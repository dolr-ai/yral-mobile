package com.yral.shared.rust.service.domain.usecases

import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.rust.service.domain.UserInfoRepository
import com.yral.shared.rust.service.domain.models.UserProfileDetails

class GetUsersProfileDetailsUseCase(
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
    private val userInfoRepository: UserInfoRepository,
) : SuspendUseCase<GetUsersProfileDetailsParams, Map<String, UserProfileDetails>>(
        appDispatchers.network,
        useCaseFailureListener,
    ) {
    override suspend fun execute(parameter: GetUsersProfileDetailsParams): Map<String, UserProfileDetails> =
        if (parameter.targetPrincipalIds.isEmpty()) {
            emptyMap()
        } else {
            userInfoRepository.getUsersProfileDetails(
                principal = parameter.callerPrincipal,
                targetPrincipalIds = parameter.targetPrincipalIds,
            )
        }
}

data class GetUsersProfileDetailsParams(
    val callerPrincipal: String,
    val targetPrincipalIds: List<String>,
)
