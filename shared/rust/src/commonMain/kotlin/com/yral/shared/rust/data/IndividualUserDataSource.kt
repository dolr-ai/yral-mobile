package com.yral.shared.rust.data

import com.yral.shared.uniffi.generated.Result12

interface IndividualUserDataSource {
    suspend fun getPostsOfThisUserProfileWithPaginationCursor(pageNo: ULong): Result12
}