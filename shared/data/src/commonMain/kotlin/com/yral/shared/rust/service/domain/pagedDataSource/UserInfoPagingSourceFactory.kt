package com.yral.shared.rust.service.domain.pagedDataSource

import com.yral.shared.rust.service.domain.UserInfoRepository

class UserInfoPagingSourceFactory(
    private val userInfoRepository: UserInfoRepository,
) {
    fun createFollowersPagingSource(
        principal: String,
        targetPrincipal: String,
        withCallerFollows: Boolean? = null,
    ): FollowersPagingSource =
        FollowersPagingSource(
            profileRepository = userInfoRepository,
            principal = principal,
            targetPrincipal = targetPrincipal,
            withCallerFollows = withCallerFollows,
        )

    fun createFollowingPagingSource(
        principal: String,
        targetPrincipal: String,
        withCallerFollows: Boolean? = null,
    ): FollowingPagingSource =
        FollowingPagingSource(
            profileRepository = userInfoRepository,
            principal = principal,
            targetPrincipal = targetPrincipal,
            withCallerFollows = withCallerFollows,
        )
}
