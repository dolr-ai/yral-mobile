package com.yral.shared.features.feed.useCases

import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.libs.useCase.SuspendUseCase
import com.yral.shared.rust.domain.IndividualUserRepository
import com.yral.shared.rust.domain.models.FeedRequest
import com.yral.shared.rust.domain.models.FilteredResult
import com.yral.shared.rust.domain.models.PostResponse

class FetchMoreFeedUseCase(
    private val individualUserRepository: IndividualUserRepository,
    appDispatchers: AppDispatchers,
    crashlyticsManager: CrashlyticsManager,
) : SuspendUseCase<FetchMoreFeedUseCase.Params, PostResponse>(appDispatchers.io, crashlyticsManager) {
    override suspend fun execute(parameter: Params): PostResponse =
        individualUserRepository.fetchMoreFeeds(
            feedRequest =
                FeedRequest(
                    userId = parameter.userId,
                    filterResults = parameter.filterResults,
                    numResults = parameter.batchSize.toLong(),
                ),
        )

    data class Params(
        val userId: String,
        val filterResults: List<FilteredResult>,
        val batchSize: Int,
    )
}
