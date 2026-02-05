package com.yral.shared.features.aiinfluencer.domain.usecases

import com.yral.shared.features.aiinfluencer.domain.AiInfluencerRepository
import com.yral.shared.features.aiinfluencer.domain.models.CreatedInfluencer
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class CreateInfluencerUseCase(
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
    private val repository: AiInfluencerRepository,
) : SuspendUseCase<CreateInfluencerUseCase.Params, CreatedInfluencer>(
        appDispatchers.network,
        useCaseFailureListener,
    ) {
    override suspend fun execute(parameter: Params): CreatedInfluencer = repository.createInfluencer(parameter.request)

    data class Params(
        val request: CreatedInfluencer,
    )
}
