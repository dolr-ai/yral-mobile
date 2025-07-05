package com.yral.shared.features.profile.domain

import com.yral.shared.features.profile.domain.models.DeleteVideoRequest
import com.yral.shared.features.profile.domain.repository.ProfileRepository
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class DeleteVideoUseCase(
    private val profileRepository: ProfileRepository,
    appDispatchers: AppDispatchers,
    failureListener: UseCaseFailureListener,
) : SuspendUseCase<DeleteVideoRequest, Unit>(appDispatchers.network, failureListener) {
    override suspend fun execute(parameter: DeleteVideoRequest) =
        profileRepository
            .deleteVideo(parameter)
}
