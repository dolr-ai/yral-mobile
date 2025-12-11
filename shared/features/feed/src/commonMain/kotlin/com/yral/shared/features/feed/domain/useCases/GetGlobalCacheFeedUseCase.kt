package com.yral.shared.features.feed.domain.useCases

import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.features.feed.domain.IFeedRepository
import com.yral.shared.features.feed.domain.models.FeedRequest
import com.yral.shared.features.feed.domain.models.PostResponse
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers

class GetGlobalCacheFeedUseCase(
    private val feedRepository: IFeedRepository,
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
) : SuspendUseCase<Unit, PostResponse>(appDispatchers.network, useCaseFailureListener) {
    override val exceptionType: String = ExceptionType.FEED.name

    override suspend fun execute(parameter: Unit): PostResponse =
        feedRepository.getInitialCachedFeeds(
            feedRequest =
                FeedRequest(
                    userId = "", // end point is not dependent on userId
                    numResults = INITIAL_REQUEST,
                ),
        )

    companion object {
        private const val INITIAL_REQUEST = 20L
    }
}
