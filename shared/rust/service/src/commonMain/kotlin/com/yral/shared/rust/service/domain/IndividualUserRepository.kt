package com.yral.shared.rust.service.domain

import com.yral.shared.data.domain.models.FeedDetails
import com.yral.shared.data.domain.models.Post
import com.yral.shared.rust.service.domain.models.Posts

interface IndividualUserRepository {
    suspend fun fetchFeedDetails(
        post: Post,
        shouldFetchFromServiceCanisters: Boolean,
    ): FeedDetails
    suspend fun fetchFeedDetailsWithCreatorInfo(post: Post): FeedDetails?
    suspend fun getPostsOfThisUserProfileWithPaginationCursor(
        canisterId: String,
        principalId: String,
        startIndex: ULong,
        pageSize: ULong,
        shouldFetchFromServiceCanisters: Boolean,
    ): Posts
    suspend fun getUserBitcoinBalance(
        canisterId: String,
        principalId: String,
    ): String
    suspend fun getUserDolrBalance(
        canisterId: String,
        principalId: String,
    ): String
}
