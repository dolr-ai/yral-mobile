package com.yral.shared.features.feed.domain.useCases

import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.features.feed.domain.IFeedRepository
import com.yral.shared.features.feed.domain.models.FeedRequest
import com.yral.shared.features.feed.domain.models.PostResponse
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class FetchMoreFeedUseCase(
    private val feedRepository: IFeedRepository,
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
) : SuspendUseCase<FetchMoreFeedUseCase.Params, PostResponse>(appDispatchers.network, useCaseFailureListener) {
    override val exceptionType: String = ExceptionType.FEED.name

    override suspend fun execute(parameter: Params): PostResponse =
        feedRepository.fetchMoreFeeds(
            feedRequest =
                FeedRequest(
                    userId = parameter.userId,
                    numResults = parameter.batchSize.toLong(),
                ),
        )

    data class Params(
        val userId: String,
        val batchSize: Int,
    )
}
