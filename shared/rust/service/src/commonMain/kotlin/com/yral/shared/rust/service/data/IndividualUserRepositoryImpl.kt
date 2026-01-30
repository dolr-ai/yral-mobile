package com.yral.shared.rust.service.data

import com.yral.shared.data.domain.models.FeedDetails
import com.yral.shared.data.domain.models.Post
import com.yral.shared.data.domain.models.toDTO
import com.yral.shared.rust.service.data.models.toPosts
import com.yral.shared.rust.service.domain.IndividualUserRepository
import com.yral.shared.rust.service.domain.models.Posts
import com.yral.shared.rust.service.domain.models.toFeedDetails
import com.yral.shared.rust.service.domain.performance.RustApiPerformanceTracer
import com.yral.shared.rust.service.domain.performance.traceApiCall

internal class IndividualUserRepositoryImpl(
    private val dataSource: IndividualUserDataSource,
    private val performanceTracer: RustApiPerformanceTracer,
) : IndividualUserRepository {
    override suspend fun fetchFeedDetails(
        post: Post,
        shouldFetchFromServiceCanisters: Boolean,
    ): FeedDetails =
        traceApiCall(performanceTracer, "fetchFeedDetails") {
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
        }

    override suspend fun getPostsOfThisUserProfileWithPaginationCursor(
        canisterId: String,
        principalId: String,
        startIndex: ULong,
        pageSize: ULong,
        shouldFetchFromServiceCanisters: Boolean,
    ): Posts =
        traceApiCall(performanceTracer, "getPostsOfThisUserProfile") {
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
        }

    override suspend fun fetchFeedDetailsWithCreatorInfo(post: Post): FeedDetails? =
        traceApiCall(performanceTracer, "fetchFeedDetailsWithCreatorInfo") {
            dataSource
                .fetchFeedDetailsWithCreatorInfo(post.toDTO())
                ?.toFeedDetails()
        }

    override suspend fun fetchPostDetailsWithNsfwInfo(post: Post): FeedDetails? =
        traceApiCall(performanceTracer, "fetchPostDetailsWithNsfwInfo") {
            dataSource
                .fetchPostDetailsWithNsfwInfo(post.toDTO())
                ?.toFeedDetails()
        }

    override suspend fun getUserBitcoinBalance(
        canisterId: String,
        principalId: String,
    ): String =
        traceApiCall(performanceTracer, "getUserBitcoinBalance") {
            dataSource.getUserBitcoinBalance(canisterId, principalId)
        }

    override suspend fun getUserDolrBalance(
        canisterId: String,
        principalId: String,
    ): String =
        traceApiCall(performanceTracer, "getUserDolrBalance") {
            dataSource.getUserDolrBalance(canisterId, principalId)
        }
}
