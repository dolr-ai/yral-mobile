package com.yral.shared.features.feed.domain.useCases

import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.rust.domain.IndividualUserRepository
import com.yral.shared.rust.domain.models.FeedDetails
import com.yral.shared.rust.domain.models.Post

class FetchFeedDetailsUseCase(
    private val individualUserRepository: IndividualUserRepository,
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
) : SuspendUseCase<Post, FeedDetails>(appDispatchers.network, useCaseFailureListener) {
    override suspend fun execute(parameter: Post): FeedDetails =
        individualUserRepository
            .fetchFeedDetails(parameter)
}
