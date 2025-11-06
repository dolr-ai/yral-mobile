package com.yral.shared.data.domain.useCases

import com.yral.shared.data.domain.CommonApis
import com.yral.shared.data.domain.models.VideoViews
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class GetVideoViewsUseCase(
    private val commonApis: CommonApis,
    appDispatchers: AppDispatchers,
    failureListener: UseCaseFailureListener,
) : SuspendUseCase<GetVideoViewsUseCase.Params, List<VideoViews>>(appDispatchers.network, failureListener) {
    override suspend fun execute(parameter: Params) =
        commonApis
            .getVideoViewsCount(videoId = parameter.videoId)

    data class Params(
        val videoId: List<String>,
    )
}
