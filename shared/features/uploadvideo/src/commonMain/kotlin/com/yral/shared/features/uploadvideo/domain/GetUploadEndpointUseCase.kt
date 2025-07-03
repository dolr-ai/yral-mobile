package com.yral.shared.features.uploadvideo.domain

import com.yral.shared.features.uploadvideo.domain.models.UploadEndpoint
import com.yral.shared.libs.arch.domain.UnitSuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

internal class GetUploadEndpointUseCase(
    appDispatchers: AppDispatchers,
    failureListener: UseCaseFailureListener,
    private val repository: UploadRepository
) : UnitSuspendUseCase<UploadEndpoint>(appDispatchers.network, failureListener) {
    override suspend fun execute(parameter: Unit) = repository.fetchUploadUrl()
}
