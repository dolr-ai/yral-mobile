package com.yral.shared.rust.domain

import com.yral.shared.data.feed.domain.FeedDetails
import com.yral.shared.data.feed.domain.Post
import com.yral.shared.rust.data.models.Posts

interface IndividualUserRepository {
    suspend fun fetchFeedDetails(
        post: Post,
        shouldFetchFromServiceCanisters: Boolean,
    ): FeedDetails
    suspend fun getPostsOfThisUserProfileWithPaginationCursor(
        principalId: String,
        startIndex: ULong,
        pageSize: ULong,
        shouldFetchFromServiceCanisters: Boolean,
    ): Posts
}
