package com.yral.shared.rust.domain

import com.yral.shared.uniffi.generated.PostDetailsForFrontend

interface IndividualUserRepository {
    suspend fun getPostsOfThisUserProfileWithPaginationCursor(pageNo: ULong): List<PostDetailsForFrontend>
}
