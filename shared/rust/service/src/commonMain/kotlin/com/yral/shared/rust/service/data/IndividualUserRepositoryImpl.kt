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
    override suspend fun fetchFeedDetails(post: Post): FeedDetails =
        traceApiCall(performanceTracer, "fetchFeedDetails") {
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
    ): Posts =
        traceApiCall(performanceTracer, "getPostsOfThisUserProfile") {
            dataSource
                .getSCPostsOfThisUserProfileWithPaginationCursor(
                    principalId = principalId,
                    startIndex = startIndex,
                    pageSize = pageSize,
                ).toPosts(canisterId)
        }

    override suspend fun getDraftPostsWithPagination(
        canisterId: String,
        startIndex: ULong,
        pageSize: ULong,
    ): Posts =
        traceApiCall(performanceTracer, "getDraftPostsWithPagination") {
            dataSource
                .getDraftPostsWithPagination(startIndex, pageSize)
                .toPosts(canisterId)
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
