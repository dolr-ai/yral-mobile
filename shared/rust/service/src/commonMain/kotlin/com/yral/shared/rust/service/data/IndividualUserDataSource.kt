package com.yral.shared.rust.service.data

import com.yral.shared.data.data.models.PostDTO
import com.yral.shared.uniffi.generated.UpsPostDetailsForFrontend
import com.yral.shared.uniffi.generated.UpsResult3

internal interface IndividualUserDataSource {
    suspend fun fetchSCFeedDetails(post: PostDTO): UpsPostDetailsForFrontend
    suspend fun getSCPostsOfThisUserProfileWithPaginationCursor(
        principalId: String,
        startIndex: ULong,
        pageSize: ULong,
    ): UpsResult3
    suspend fun getDraftPostsWithPagination(
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
