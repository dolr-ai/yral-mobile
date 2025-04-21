package com.yral.shared.rust.data

import com.yral.shared.rust.services.IndividualUserServiceFactory
import com.yral.shared.uniffi.generated.Result12

class IndividualUserDataSourceImpl(
    private val individualUserServiceFactory: IndividualUserServiceFactory,
) : IndividualUserDataSource {
    override suspend fun getPostsOfThisUserProfileWithPaginationCursor(pageNo: ULong): Result12 =
        individualUserServiceFactory
            .service()
            .getPostsOfThisUserProfileWithPaginationCursor(pageNo, PAGE_SIZE)

    companion object {
        const val PAGE_SIZE = 10UL
    }
}
