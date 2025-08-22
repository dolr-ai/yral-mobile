package com.yral.shared.features.feed.domain.useCases

import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.rust.domain.IndividualUserRepository
import com.yral.shared.rust.domain.models.FeedRequest
import com.yral.shared.rust.domain.models.PostResponse

class FetchMoreFeedUseCase(
    private val individualUserRepository: IndividualUserRepository,
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
) : SuspendUseCase<FetchMoreFeedUseCase.Params, PostResponse>(appDispatchers.network, useCaseFailureListener) {
    override suspend fun execute(parameter: Params): PostResponse =
        individualUserRepository.fetchMoreFeeds(
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
