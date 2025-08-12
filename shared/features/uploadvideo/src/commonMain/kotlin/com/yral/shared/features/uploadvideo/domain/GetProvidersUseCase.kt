package com.yral.shared.features.uploadvideo.domain

import com.yral.shared.features.uploadvideo.domain.models.Provider
import com.yral.shared.libs.arch.domain.UnitSuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

internal class GetProvidersUseCase(
    appDispatchers: AppDispatchers,
    failureListener: UseCaseFailureListener,
    private val repository: UploadRepository,
) : UnitSuspendUseCase<List<Provider>>(appDispatchers.network, failureListener) {
    override suspend fun execute(parameter: Unit): List<Provider> = repository.fetchProviders()
}
