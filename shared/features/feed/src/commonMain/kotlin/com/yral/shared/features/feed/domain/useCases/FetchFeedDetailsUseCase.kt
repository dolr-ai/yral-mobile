package com.yral.shared.features.feed.domain.useCases

import com.yral.shared.data.feed.domain.FeedDetails
import com.yral.shared.data.feed.domain.Post
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
    override suspend fun execute(parameter: Post): FeedDetails {
        var isFromServiceCanister = getUserInfoServiceCanister() == parameter.canisterID
        if (!isFromServiceCanister) {
            val postIdAsUInt = parameter.postID.toUIntOrNull()
            if (postIdAsUInt == null) {
                isFromServiceCanister = true
            }
        }
        return individualUserRepository
            .fetchFeedDetails(parameter, isFromServiceCanister)
    }
}
