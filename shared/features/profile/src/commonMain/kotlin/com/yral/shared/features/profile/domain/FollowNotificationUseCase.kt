package com.yral.shared.features.profile.domain

import com.yral.shared.features.profile.domain.models.FollowNotification
import com.yral.shared.features.profile.domain.repository.ProfileRepository
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class FollowNotificationUseCase(
    private val profileRepository: ProfileRepository,
    appDispatchers: AppDispatchers,
    failureListener: UseCaseFailureListener,
) : SuspendUseCase<FollowNotificationUseCase.Params, Unit>(appDispatchers.network, failureListener) {
    override suspend fun execute(parameter: Params) {
        profileRepository
            .followNotification(
                request =
                    FollowNotification(
                        followerUsername = parameter.followerUsername,
                        targetPrincipal = parameter.targetPrincipal,
                    ),
            )
    }

    data class Params(
        val followerUsername: String,
        val targetPrincipal: String,
    )
}
