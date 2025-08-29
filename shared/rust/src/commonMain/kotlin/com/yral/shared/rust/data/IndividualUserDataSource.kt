package com.yral.shared.rust.data

import com.yral.shared.data.feed.data.PostDTO
import com.yral.shared.uniffi.generated.PostDetailsForFrontend
import com.yral.shared.uniffi.generated.Result12

interface IndividualUserDataSource {
    suspend fun fetchFeedDetails(post: PostDTO): PostDetailsForFrontend
    suspend fun getPostsOfThisUserProfileWithPaginationCursor(
        principalId: String,
        startIndex: ULong,
        pageSize: ULong,
    ): Result12
}
