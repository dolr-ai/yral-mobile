package com.yral.shared.features.profile.videoideas.domain.usecases

import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.features.profile.videoideas.domain.VideoIdeasRepository
import com.yral.shared.features.profile.videoideas.domain.models.VideoIdea
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

private val EXCEPTION_TYPE = ExceptionType.CHAT.name

class GetVideoIdeasUseCase(
    private val repository: VideoIdeasRepository,
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
) : SuspendUseCase<String, List<VideoIdea>>(appDispatchers.network, useCaseFailureListener) {
    override val exceptionType: String = EXCEPTION_TYPE

    override suspend fun execute(parameter: String): List<VideoIdea> = repository.listIdeas(parameter)
}

class MarkVideoIdeaUsedUseCase(
    private val repository: VideoIdeasRepository,
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
) : SuspendUseCase<MarkVideoIdeaUsedUseCase.Params, VideoIdea>(appDispatchers.network, useCaseFailureListener) {
    override val exceptionType: String = EXCEPTION_TYPE

    override suspend fun execute(parameter: Params): VideoIdea =
        repository.markIdeaUsed(parameter.influencerId, parameter.ideaId)

    data class Params(
        val influencerId: String,
        val ideaId: String,
    )
}
