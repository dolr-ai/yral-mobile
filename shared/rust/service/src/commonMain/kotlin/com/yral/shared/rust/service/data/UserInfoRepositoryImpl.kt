package com.yral.shared.rust.service.data

import com.yral.shared.rust.service.domain.UserInfoRepository
import com.yral.shared.uniffi.generated.Principal
import com.yral.shared.uniffi.generated.UisFollowersResponse
import com.yral.shared.uniffi.generated.UisFollowingResponse
import com.yral.shared.uniffi.generated.UisUserProfileDetailsForFrontendV4

class UserInfoRepositoryImpl(
    private val dataSource: UserInfoDataSource,
) : UserInfoRepository {
    override suspend fun followUser(
        principal: Principal,
        targetPrincipal: Principal,
    ): Unit = dataSource.followUser(principal, targetPrincipal)

    override suspend fun unfollowUser(
        principal: Principal,
        targetPrincipal: Principal,
    ): Unit = dataSource.unfollowUser(principal, targetPrincipal)

    override suspend fun getProfileDetailsV4(
        principal: Principal,
        targetPrincipal: Principal,
    ): UisUserProfileDetailsForFrontendV4 = dataSource.getProfileDetailsV4(principal, targetPrincipal)

    override suspend fun getFollowers(
        principal: Principal,
        targetPrincipal: Principal,
        cursorPrincipal: Principal?,
        limit: ULong,
        withCallerFollows: Boolean?,
    ): UisFollowersResponse =
        dataSource.getFollowers(
            principal = principal,
            targetPrincipal = targetPrincipal,
            cursorPrincipal = cursorPrincipal,
            limit = limit,
            withCallerFollows = withCallerFollows,
        )

    override suspend fun getFollowing(
        principal: Principal,
        targetPrincipal: Principal,
        cursorPrincipal: Principal?,
        limit: ULong,
        withCallerFollows: Boolean?,
    ): UisFollowingResponse =
        dataSource.getFollowing(
            principal = principal,
            targetPrincipal = targetPrincipal,
            cursorPrincipal = cursorPrincipal,
            limit = limit,
            withCallerFollows = withCallerFollows,
        )
}
