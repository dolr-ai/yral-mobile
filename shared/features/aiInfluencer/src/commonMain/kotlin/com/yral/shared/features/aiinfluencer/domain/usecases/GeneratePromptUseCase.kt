package com.yral.shared.features.aiinfluencer.domain.usecases

import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.features.aiinfluencer.domain.AiInfluencerRepository
import com.yral.shared.features.aiinfluencer.domain.models.GeneratedPrompt
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class GeneratePromptUseCase(
    private val repository: AiInfluencerRepository,
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
) : SuspendUseCase<GeneratePromptUseCase.Params, GeneratedPrompt>(
        appDispatchers.network,
        useCaseFailureListener,
    ) {
    override val exceptionType: String = ExceptionType.CHAT.name

    override suspend fun execute(parameter: Params): GeneratedPrompt =
        repository.generatePrompt(
            prompt = parameter.prompt,
        )

    data class Params(
        val prompt: String,
    )
}
