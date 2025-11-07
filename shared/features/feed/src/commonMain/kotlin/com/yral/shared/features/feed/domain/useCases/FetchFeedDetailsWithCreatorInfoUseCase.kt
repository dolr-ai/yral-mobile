package com.yral.shared.features.feed.domain.useCases

import com.yral.shared.data.domain.models.FeedDetails
import com.yral.shared.data.domain.models.Post
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.rust.service.domain.IndividualUserRepository

class FetchFeedDetailsWithCreatorInfoUseCase(
    private val individualUserRepository: IndividualUserRepository,
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
) : SuspendUseCase<Post, FeedDetails?>(appDispatchers.network, useCaseFailureListener) {
    override suspend fun execute(parameter: Post): FeedDetails? =
        individualUserRepository
            .fetchFeedDetailsWithCreatorInfo(parameter)
}
