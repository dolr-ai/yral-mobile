package com.yral.shared.rust.service.data

import com.yral.shared.data.data.models.PostDTO
import com.yral.shared.data.domain.models.FeedDetails
import com.yral.shared.rust.service.domain.models.Posts

internal interface IndividualUserDataSource {
    suspend fun fetchSCFeedDetails(post: PostDTO): FeedDetails
    suspend fun getSCPostsOfThisUserProfileWithPaginationCursor(
        principalId: String,
        startIndex: ULong,
        pageSize: ULong,
    ): Posts
    suspend fun getDraftPostsWithPagination(
        startIndex: ULong,
        pageSize: ULong,
    ): Posts
}
