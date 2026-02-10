package com.yral.shared.features.feed.domain.useCases

import com.yral.shared.core.session.SessionManager
import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.data.domain.models.FeedDetails
import com.yral.shared.data.domain.models.Post
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.rust.service.domain.IndividualUserRepository
import com.yral.shared.rust.service.domain.UserInfoRepository
import com.yral.shared.rust.service.domain.models.SubscriptionPlan
import com.yral.shared.rust.service.utils.getUserInfoServiceCanister
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class FetchFeedDetailsWithCreatorInfoUseCase(
    private val individualUserRepository: IndividualUserRepository,
    private val userInfoRepository: UserInfoRepository,
    private val sessionManager: SessionManager,
    appDispatchers: AppDispatchers,
    useCaseFailureListener: UseCaseFailureListener,
) : SuspendUseCase<Post, FeedDetails?>(appDispatchers.network, useCaseFailureListener) {
    override val exceptionType: String = ExceptionType.FEED.name

    override suspend fun execute(parameter: Post): FeedDetails =
        coroutineScope {
            val isFromServiceCanister = getUserInfoServiceCanister() == parameter.canisterID
            val detailsDeferred =
                async {
                    individualUserRepository.fetchFeedDetails(parameter, isFromServiceCanister)
                }
            val profileDeferred =
                async {
                    val principal = sessionManager.userPrincipal ?: return@async null
                    userInfoRepository.getUserProfileDetailsV7(
                        principal = principal,
                        targetPrincipal = parameter.publisherUserId,
                    )
                }

            val details = detailsDeferred.await()
            val profile = profileDeferred.await()

            val profileImageUrl =
                profile
                    ?.profilePictureUrl
                    ?.takeIf { it.isNotBlank() }
                    ?: details.profileImageURL
            val isFollowing =
                profile
                    ?.userFollowsCaller
                    ?: details.isFollowing

            details.copy(
                profileImageURL = profileImageUrl,
                isFollowing = isFollowing,
                isProUser = profile?.subscriptionPlan is SubscriptionPlan.Pro,
                isAiInfluencer = profile?.isAiInfluencer,
            )
        }
}
