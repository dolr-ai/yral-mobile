package com.yral.shared.features.feed.domain.useCases

import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.rust.domain.IndividualUserRepository
import com.yral.shared.rust.domain.models.FeedRequest
import com.yral.shared.rust.domain.models.PostResponse

class GetInitialFeedUseCase(
    private val individualUserRepository: IndividualUserRepository,
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
) : SuspendUseCase<GetInitialFeedUseCase.Params, PostResponse>(appDispatchers.network, useCaseFailureListener) {
    override suspend fun execute(parameter: Params): PostResponse =
        individualUserRepository.getInitialFeeds(
            feedRequest =
                FeedRequest(
                    userId = parameter.userId,
                    numResults = INITIAL_REQUEST,
                ),
        )

    companion object {
        private const val INITIAL_REQUEST = 20L
    }

    data class Params(
        val userId: String,
    )
}
