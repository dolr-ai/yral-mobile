package com.yral.shared.rust.service.data

import com.yral.shared.data.feed.domain.FeedDetails
import com.yral.shared.data.feed.domain.Post
import com.yral.shared.data.feed.domain.toDTO
import com.yral.shared.rust.service.domain.IndividualUserRepository
import com.yral.shared.rust.service.domain.models.Posts
import com.yral.shared.rust.service.domain.models.toFeedDetails
import com.yral.shared.rust.service.domain.models.toPosts

internal class IndividualUserRepositoryImpl(
    private val dataSource: IndividualUserDataSource,
) : IndividualUserRepository {
    override suspend fun fetchFeedDetails(
        post: Post,
        shouldFetchFromServiceCanisters: Boolean,
    ): FeedDetails =
        if (!shouldFetchFromServiceCanisters) {
            dataSource
                .fetchFeedDetails(post.toDTO())
                .toFeedDetails(
                    postId = post.postID,
                    canisterId = post.canisterID,
                    nsfwProbability = post.nsfwProbability,
                )
        } else {
            dataSource
                .fetchSCFeedDetails(post.toDTO())
                .toFeedDetails(
                    postId = post.postID,
                    canisterId = post.canisterID,
                    nsfwProbability = post.nsfwProbability,
                )
        }

    override suspend fun getPostsOfThisUserProfileWithPaginationCursor(
        canisterId: String,
        principalId: String,
        startIndex: ULong,
        pageSize: ULong,
        shouldFetchFromServiceCanisters: Boolean,
    ): Posts =
        if (!shouldFetchFromServiceCanisters) {
            dataSource
                .getPostsOfThisUserProfileWithPaginationCursor(
                    canisterId = canisterId,
                    startIndex = startIndex,
                    pageSize = pageSize,
                ).toPosts(canisterId)
        } else {
            dataSource
                .getSCPostsOfThisUserProfileWithPaginationCursor(
                    principalId = principalId,
                    startIndex = startIndex,
                    pageSize = pageSize,
                ).toPosts(canisterId)
        }

    override suspend fun getUserBitcoinBalance(
        canisterId: String,
        principalId: String,
    ): String =
        dataSource
            .getUserBitcoinBalance(canisterId, principalId)

    override suspend fun getUserDolrBalance(
        canisterId: String,
        principalId: String,
    ): String =
        dataSource
            .getUserDolrBalance(canisterId, principalId)
}
