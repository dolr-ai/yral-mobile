package com.yral.shared.features.uploadvideo.domain

import com.yral.shared.features.uploadvideo.domain.models.GenerateVideoParams
import com.yral.shared.features.uploadvideo.domain.models.GenerateVideoResult
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

internal class GenerateVideoUseCase(
    appDispatchers: AppDispatchers,
    failureListener: UseCaseFailureListener,
    private val repository: UploadRepository,
) : SuspendUseCase<GenerateVideoUseCase.Param, GenerateVideoResult>(
        coroutineDispatcher = appDispatchers.network,
        failureListener = failureListener,
    ) {
    override suspend fun execute(parameter: Param): GenerateVideoResult = repository.generateVideo(parameter.params)

    data class Param(
        val params: GenerateVideoParams,
    )
}
