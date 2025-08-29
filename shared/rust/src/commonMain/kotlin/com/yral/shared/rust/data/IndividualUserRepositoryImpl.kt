package com.yral.shared.rust.data

import com.yral.shared.data.feed.domain.FeedDetails
import com.yral.shared.data.feed.domain.Post
import com.yral.shared.data.feed.domain.toDTO
import com.yral.shared.rust.data.models.toFeedDetails
import com.yral.shared.rust.domain.IndividualUserRepository
import com.yral.shared.uniffi.generated.Result12

class IndividualUserRepositoryImpl(
    private val dataSource: IndividualUserDataSource,
) : IndividualUserRepository {
    override suspend fun fetchFeedDetails(post: Post): FeedDetails =
        dataSource
            .fetchFeedDetails(post.toDTO())
            .toFeedDetails(
                postId = post.postID,
                canisterId = post.canisterID,
                nsfwProbability = post.nsfwProbability,
            )

    override suspend fun getPostsOfThisUserProfileWithPaginationCursor(
        principalId: String,
        startIndex: ULong,
        pageSize: ULong,
    ): Result12 =
        dataSource
            .getPostsOfThisUserProfileWithPaginationCursor(
                principalId = principalId,
                startIndex = startIndex,
                pageSize = pageSize,
            )
}
