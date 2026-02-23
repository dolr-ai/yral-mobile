package com.yral.shared.features.aiinfluencer.domain.usecases

import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.features.aiinfluencer.domain.AiInfluencerRepository
import com.yral.shared.features.aiinfluencer.domain.models.GeneratedInfluencerMetadata
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class ValidateAndGenerateMetadataUseCase(
    private val repository: AiInfluencerRepository,
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
) : SuspendUseCase<ValidateAndGenerateMetadataUseCase.Params, GeneratedInfluencerMetadata>(
        appDispatchers.network,
        useCaseFailureListener,
    ) {
    override val exceptionType: String = ExceptionType.CHAT.name

    override suspend fun execute(parameter: Params): GeneratedInfluencerMetadata =
        repository.validateAndGenerateMetadata(systemInstructions = parameter.systemInstructions)

    data class Params(
        val systemInstructions: String,
    )
}
