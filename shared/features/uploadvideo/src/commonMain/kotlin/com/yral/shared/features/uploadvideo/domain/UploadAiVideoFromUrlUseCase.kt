package com.yral.shared.features.uploadvideo.domain

import com.yral.shared.features.uploadvideo.domain.models.UploadAiVideoFromUrlRequest
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

internal class UploadAiVideoFromUrlUseCase(
    appDispatchers: AppDispatchers,
    failureListener: UseCaseFailureListener,
    private val repository: UploadRepository,
) : SuspendUseCase<UploadAiVideoFromUrlUseCase.Params, String>(
        coroutineDispatcher = appDispatchers.network,
        failureListener = failureListener,
    ) {
    override suspend fun execute(parameter: Params): String =
        repository.uploadAiVideoFromUrl(
            request =
                UploadAiVideoFromUrlRequest(
                    videoUrl = parameter.videoUrl,
                    hashtags = parameter.hashtags,
                    description = parameter.description,
                    isNsfw = parameter.isNsfw,
                    enableHotOrNot = parameter.enableHotOrNot,
                ),
        )

    data class Params(
        val videoUrl: String,
        val hashtags: List<String>,
        val description: String,
        val isNsfw: Boolean,
        val enableHotOrNot: Boolean,
    )
}
