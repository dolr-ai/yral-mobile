package com.yral.shared.features.feed.domain.useCases

import com.yral.shared.features.feed.domain.IFeedRepository
import com.yral.shared.features.feed.domain.models.AIFeedRequest
import com.yral.shared.features.feed.domain.models.PostResponse
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class GetAIFeedUseCase(
    private val feedRepository: IFeedRepository,
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
) : SuspendUseCase<GetAIFeedUseCase.Params, PostResponse>(appDispatchers.network, useCaseFailureListener) {
    override suspend fun execute(parameter: Params): PostResponse =
        feedRepository.fetchAIFeeds(
            feedRequest =
                AIFeedRequest(
                    userId = parameter.userId,
                    count = INITIAL_REQUEST,
                ),
        )

    companion object {
        private const val INITIAL_REQUEST = 20
    }

    data class Params(
        val userId: String,
    )
}
