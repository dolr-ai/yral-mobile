package com.yral.shared.features.feed.useCases

import com.yral.shared.core.AppDispatchers
import com.yral.shared.core.SuspendUseCase
import com.yral.shared.rust.domain.IndividualUserRepository
import com.yral.shared.rust.domain.models.FeedDetails
import com.yral.shared.rust.domain.models.Post

class FetchFeedDetailsUseCase(
    private val individualUserRepository: IndividualUserRepository,
    appDispatchers: AppDispatchers,
) : SuspendUseCase<Post, FeedDetails>(appDispatchers.io) {
    override suspend fun execute(parameter: Post): FeedDetails =
        individualUserRepository
            .fetchFeedDetails(parameter)
}
