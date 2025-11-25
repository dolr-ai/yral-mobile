package com.yral.shared.features.feed.domain.useCases

import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.data.domain.models.FeedDetails
import com.yral.shared.data.domain.models.Post
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.rust.service.domain.IndividualUserRepository
import com.yral.shared.rust.service.utils.getUserInfoServiceCanister

class FetchFeedDetailsUseCase(
    private val individualUserRepository: IndividualUserRepository,
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
) : SuspendUseCase<Post, FeedDetails>(appDispatchers.network, useCaseFailureListener) {
    override val exceptionType: String = ExceptionType.FEED.name

    override suspend fun execute(parameter: Post): FeedDetails {
        val isFromServiceCanister = getUserInfoServiceCanister() == parameter.canisterID
        return individualUserRepository
            .fetchFeedDetails(parameter, isFromServiceCanister)
    }
}
