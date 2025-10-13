package com.yral.shared.rust.service.data

import com.yral.shared.rust.service.domain.UserInfoRepository
import com.yral.shared.rust.service.domain.models.FollowersPageResult
import com.yral.shared.rust.service.domain.models.FollowingPageResult
import com.yral.shared.rust.service.domain.models.toFollowerPageResult
import com.yral.shared.rust.service.domain.models.toFollowingPageResult
import com.yral.shared.uniffi.generated.Principal
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
    ): FollowersPageResult =
        dataSource
            .getFollowers(
                principal = principal,
                targetPrincipal = targetPrincipal,
                cursorPrincipal = cursorPrincipal,
                limit = limit,
                withCallerFollows = withCallerFollows,
            ).toFollowerPageResult()

    override suspend fun getFollowing(
        principal: Principal,
        targetPrincipal: Principal,
        cursorPrincipal: Principal?,
        limit: ULong,
        withCallerFollows: Boolean?,
    ): FollowingPageResult =
        dataSource
            .getFollowing(
                principal = principal,
                targetPrincipal = targetPrincipal,
                cursorPrincipal = cursorPrincipal,
                limit = limit,
                withCallerFollows = withCallerFollows,
            ).toFollowingPageResult()
}
