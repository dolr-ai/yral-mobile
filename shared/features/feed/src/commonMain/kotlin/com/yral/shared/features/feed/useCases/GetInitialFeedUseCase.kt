package com.yral.shared.features.feed.useCases

import com.yral.shared.core.SuspendUseCase
import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.rust.domain.IndividualUserRepository
import com.yral.shared.rust.domain.models.FeedRequest
import com.yral.shared.rust.domain.models.FilteredResult
import com.yral.shared.rust.domain.models.PostResponse

class GetInitialFeedUseCase(
    private val individualUserRepository: IndividualUserRepository,
    appDispatchers: AppDispatchers,
) : SuspendUseCase<GetInitialFeedUseCase.Params, PostResponse>(appDispatchers.io) {
    override suspend fun execute(parameter: Params): PostResponse =
        individualUserRepository.getInitialFeeds(
            feedRequest =
                FeedRequest(
                    canisterID = parameter.canisterID,
                    filterResults = parameter.filterResults,
                    numResults = INITIAL_REQUEST,
                ),
        )

    companion object {
        private const val INITIAL_REQUEST = 20L
    }

    data class Params(
        val canisterID: String,
        val filterResults: List<FilteredResult>,
    )
}
