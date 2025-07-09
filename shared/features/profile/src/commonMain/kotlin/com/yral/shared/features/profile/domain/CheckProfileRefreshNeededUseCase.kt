package com.yral.shared.features.profile.domain

import com.yral.shared.core.session.SessionManager
import com.yral.shared.features.profile.domain.repository.ProfileRepository
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.rust.data.models.toFeedDetails

class CheckProfileRefreshNeededUseCase(
    private val profileRepository: ProfileRepository,
    private val sessionManager: SessionManager,
    appDispatchers: AppDispatchers,
    failureListener: UseCaseFailureListener,
) : SuspendUseCase<CheckProfileRefreshNeededUseCase.Params, Boolean>(appDispatchers.network, failureListener) {
    override suspend fun execute(parameter: Params): Boolean {
        val result =
            profileRepository.getProfileVideos(
                startIndex = 0UL,
                pageSize = parameter.pageSize,
            )

        if (result.posts.isEmpty()) {
            // If no videos available remotely, check if we have any cached
            return parameter.currentFirstVideoId != null
        }

        val canisterId = sessionManager.getCanisterPrincipal() ?: ""
        val firstPost = result.posts.first()
        val firstFeedDetails =
            firstPost.toFeedDetails(
                postId = firstPost.id.toLong(),
                canisterId = canisterId,
                nsfwProbability = 0.0,
            )

        val freshFirstVideoId = firstFeedDetails.videoID
        val currentFirstVideoId = parameter.currentFirstVideoId

        // Return true if video IDs don't match (refresh needed)
        return currentFirstVideoId != freshFirstVideoId
    }

    data class Params(
        val currentFirstVideoId: String?,
        val pageSize: ULong,
    )
}
