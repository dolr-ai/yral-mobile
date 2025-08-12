package com.yral.shared.rust.data

import com.yral.shared.rust.data.models.toFeedDetails
import com.yral.shared.rust.data.models.toPostResponse
import com.yral.shared.rust.domain.IndividualUserRepository
import com.yral.shared.rust.domain.models.FeedDetails
import com.yral.shared.rust.domain.models.FeedRequest
import com.yral.shared.rust.domain.models.Post
import com.yral.shared.rust.domain.models.PostResponse
import com.yral.shared.rust.domain.models.toDTO
import com.yral.shared.uniffi.generated.VideoGenRequestKey
import com.yral.shared.uniffi.generated.VideoGenRequestStatus

class IndividualUserRepositoryImpl(
    private val dataSource: IndividualUserDataSource,
) : IndividualUserRepository {
    override suspend fun getInitialFeeds(feedRequest: FeedRequest): PostResponse =
        dataSource.getInitialFeeds(feedRequest.toDTO()).toPostResponse()

    override suspend fun fetchMoreFeeds(feedRequest: FeedRequest): PostResponse =
        dataSource.fetchMoreFeeds(feedRequest.toDTO()).toPostResponse()

    override suspend fun fetchFeedDetails(post: Post): FeedDetails =
        dataSource
            .fetchFeedDetails(post.toDTO())
            .toFeedDetails(
                postId = post.postID,
                canisterId = post.canisterID,
                nsfwProbability = post.nsfwProbability,
                isNsfw = post.isNSFW,
            )

    override suspend fun fetchVideoGenerationStatus(requestKey: VideoGenRequestKey): VideoGenRequestStatus =
        dataSource
            .fetchVideoGenerationStatus(requestKey)
}
