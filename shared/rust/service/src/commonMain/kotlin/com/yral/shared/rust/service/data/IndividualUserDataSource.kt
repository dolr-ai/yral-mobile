package com.yral.shared.rust.service.data

import com.yral.shared.data.feed.data.PostDTO
import com.yral.shared.uniffi.generated.PostDetailsForFrontend
import com.yral.shared.uniffi.generated.Result12
import com.yral.shared.uniffi.generated.UpsPostDetailsForFrontend
import com.yral.shared.uniffi.generated.UpsResult3

internal interface IndividualUserDataSource {
    suspend fun fetchFeedDetails(post: PostDTO): PostDetailsForFrontend
    suspend fun fetchSCFeedDetails(post: PostDTO): UpsPostDetailsForFrontend
    suspend fun getPostsOfThisUserProfileWithPaginationCursor(
        canisterId: String,
        startIndex: ULong,
        pageSize: ULong,
    ): Result12
    suspend fun getSCPostsOfThisUserProfileWithPaginationCursor(
        principalId: String,
        startIndex: ULong,
        pageSize: ULong,
    ): UpsResult3
    suspend fun getUserBitcoinBalance(
        canisterId: String,
        principalId: String,
    ): String
    suspend fun getUserDolrBalance(
        canisterId: String,
        principalId: String,
    ): String
}
