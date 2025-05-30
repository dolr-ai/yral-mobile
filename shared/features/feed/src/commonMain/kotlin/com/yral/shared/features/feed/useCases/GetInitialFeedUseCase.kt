package com.yral.shared.features.feed.useCases

import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.libs.useCase.SuspendUseCase
import com.yral.shared.rust.domain.IndividualUserRepository
import com.yral.shared.rust.domain.models.FeedRequest
import com.yral.shared.rust.domain.models.FilteredResult
import com.yral.shared.rust.domain.models.PostResponse

class GetInitialFeedUseCase(
    private val individualUserRepository: IndividualUserRepository,
    appDispatchers: AppDispatchers,
    crashlyticsManager: CrashlyticsManager,
) : SuspendUseCase<GetInitialFeedUseCase.Params, PostResponse>(appDispatchers.io, crashlyticsManager) {
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
        const val INITIAL_REQUEST = 20L
    }

    data class Params(
        val canisterID: String,
        val filterResults: List<FilteredResult>,
    )
}
