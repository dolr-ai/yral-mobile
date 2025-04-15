package com.yral.shared.rust.data

import com.yral.shared.rust.domain.IndividualUserRepository
import com.yral.shared.uniffi.generated.PostDetailsForFrontend
import com.yral.shared.uniffi.generated.Result12

class IndividualUserRepositoryImpl(
    private val dataSource: IndividualUserDataSource,
) : IndividualUserRepository {
    override suspend fun getPostsOfThisUserProfileWithPaginationCursor(pageNo: ULong): List<PostDetailsForFrontend> {
        val result =
            dataSource.getPostsOfThisUserProfileWithPaginationCursor(
                pageNo = pageNo,
            )
        return if (result is Result12.Ok) {
            result.v1
        } else {
            emptyList()
        }
    }
}
