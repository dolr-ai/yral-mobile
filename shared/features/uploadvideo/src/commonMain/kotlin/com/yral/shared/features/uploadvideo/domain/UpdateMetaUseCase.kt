package com.yral.shared.features.uploadvideo.domain

import com.yral.shared.features.uploadvideo.domain.models.UploadFileRequest
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

internal class UpdateMetaUseCase(
    appDispatchers: AppDispatchers,
    failureListener: UseCaseFailureListener,
    private val repository: UploadRepository
) : SuspendUseCase<UpdateMetaUseCase.Param, Unit>(appDispatchers.network, failureListener) {
    override suspend fun execute(parameter: Param) {
        repository.updateMetadata(parameter.uploadFileRequest)
    }

    data class Param(
        val uploadFileRequest: UploadFileRequest
    )
}
