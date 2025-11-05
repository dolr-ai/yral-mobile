package com.yral.shared.features.profile.domain

import com.yral.shared.data.feed.domain.VideoViews
import com.yral.shared.features.profile.domain.repository.ProfileRepository
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class GetProfileVideoViewsUseCase(
    private val profileRepository: ProfileRepository,
    appDispatchers: AppDispatchers,
    failureListener: UseCaseFailureListener,
) : SuspendUseCase<GetProfileVideoViewsUseCase.Params, List<VideoViews>>(appDispatchers.network, failureListener) {
    override suspend fun execute(parameter: Params) =
        profileRepository
            .getProfileVideoViewsCount(videoId = parameter.videoId)

    data class Params(
        val videoId: List<String>,
    )
}
