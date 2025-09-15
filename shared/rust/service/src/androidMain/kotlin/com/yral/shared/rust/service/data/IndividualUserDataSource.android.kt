package com.yral.shared.rust.service.data

import com.yral.shared.data.feed.data.PostDTO
import com.yral.shared.uniffi.generated.PostDetailsForFrontend
import com.yral.shared.uniffi.generated.Result12
import com.yral.shared.uniffi.generated.ScPostDetailsForFrontend
import com.yral.shared.uniffi.generated.ScResult3

internal actual interface IndividualUserDataSource {
    suspend fun fetchFeedDetails(post: PostDTO): PostDetailsForFrontend
    suspend fun fetchSCFeedDetails(post: PostDTO): ScPostDetailsForFrontend
    suspend fun getPostsOfThisUserProfileWithPaginationCursor(
        canisterId: String,
        startIndex: ULong,
        pageSize: ULong,
    ): Result12
    suspend fun getSCPostsOfThisUserProfileWithPaginationCursor(
        principalId: String,
        startIndex: ULong,
        pageSize: ULong,
    ): ScResult3
    suspend fun getUserBitcoinBalance(principalId: String): String
}
