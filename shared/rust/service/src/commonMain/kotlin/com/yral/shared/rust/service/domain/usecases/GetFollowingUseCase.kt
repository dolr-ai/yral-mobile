package com.yral.shared.rust.service.domain.usecases

import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.uniffi.generated.Principal
import com.yral.shared.uniffi.generated.UisFollowingResponse

class GetFollowingUseCase(
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
    private val userInfoRepository: com.yral.shared.rust.service.domain.UserInfoRepository,
) : SuspendUseCase<GetFollowingParams, UisFollowingResponse>(appDispatchers.network, useCaseFailureListener) {
    override suspend fun execute(parameter: GetFollowingParams): UisFollowingResponse =
        userInfoRepository.getFollowing(
            principal = parameter.principal,
            targetPrincipal = parameter.targetPrincipal,
            cursorPrincipal = parameter.cursorPrincipal,
            limit = parameter.limit,
            withCallerFollows = parameter.withCallerFollows,
        )
}

data class GetFollowingParams(
    val principal: Principal,
    val targetPrincipal: Principal,
    val cursorPrincipal: Principal?,
    val limit: ULong,
    val withCallerFollows: Boolean?,
)
