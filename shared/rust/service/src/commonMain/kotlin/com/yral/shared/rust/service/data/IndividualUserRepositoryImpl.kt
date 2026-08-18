package com.yral.shared.rust.service.data

import com.yral.shared.data.domain.models.FeedDetails
import com.yral.shared.data.domain.models.Post
import com.yral.shared.data.domain.models.toDTO
import com.yral.shared.rust.service.domain.IndividualUserRepository
import com.yral.shared.rust.service.domain.models.Posts
import com.yral.shared.rust.service.domain.performance.RustApiPerformanceTracer
import com.yral.shared.rust.service.domain.performance.traceApiCall

internal class IndividualUserRepositoryImpl(
    private val dataSource: IndividualUserDataSource,
    private val performanceTracer: RustApiPerformanceTracer,
) : IndividualUserRepository {
    override suspend fun fetchFeedDetails(post: Post): FeedDetails =
        traceApiCall(performanceTracer, "fetchFeedDetails") {
            dataSource.fetchSCFeedDetails(post.toDTO())
        }

    override suspend fun getPostsOfThisUserProfileWithPaginationCursor(
        canisterId: String,
        principalId: String,
        startIndex: ULong,
        pageSize: ULong,
    ): Posts =
        traceApiCall(performanceTracer, "getPostsOfThisUserProfile") {
            dataSource.getSCPostsOfThisUserProfileWithPaginationCursor(
                principalId = principalId,
                startIndex = startIndex,
                pageSize = pageSize,
            )
        }

    override suspend fun getDraftPostsWithPagination(
        canisterId: String,
        startIndex: ULong,
        pageSize: ULong,
    ): Posts =
        traceApiCall(performanceTracer, "getDraftPostsWithPagination") {
            dataSource.getDraftPostsWithPagination(startIndex, pageSize)
        }
}
