package com.yral.shared.rust.domain

import com.yral.shared.data.feed.domain.FeedDetails
import com.yral.shared.data.feed.domain.Post
import com.yral.shared.uniffi.generated.Result12

interface IndividualUserRepository {
    suspend fun fetchFeedDetails(post: Post): FeedDetails
    suspend fun getPostsOfThisUserProfileWithPaginationCursor(
        principalId: String,
        startIndex: ULong,
        pageSize: ULong,
    ): Result12
}
