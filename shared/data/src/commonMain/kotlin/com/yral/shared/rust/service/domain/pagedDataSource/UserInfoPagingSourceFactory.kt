package com.yral.shared.rust.service.domain.pagedDataSource

import com.yral.shared.rust.service.domain.UserInfoRepository
import com.yral.shared.uniffi.generated.Principal

class UserInfoPagingSourceFactory(
    private val userInfoRepository: UserInfoRepository,
) {
    fun createFollowersPagingSource(
        principal: Principal,
        targetPrincipal: Principal,
        withCallerFollows: Boolean? = null,
    ): FollowersPagingSource =
        FollowersPagingSource(
            profileRepository = userInfoRepository,
            principal = principal,
            targetPrincipal = targetPrincipal,
            withCallerFollows = withCallerFollows,
        )

    fun createFollowingPagingSource(
        principal: Principal,
        targetPrincipal: Principal,
        withCallerFollows: Boolean? = null,
    ): FollowingPagingSource =
        FollowingPagingSource(
            profileRepository = userInfoRepository,
            principal = principal,
            targetPrincipal = targetPrincipal,
            withCallerFollows = withCallerFollows,
        )
}
