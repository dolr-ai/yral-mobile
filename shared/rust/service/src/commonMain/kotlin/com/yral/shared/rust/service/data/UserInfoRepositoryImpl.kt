package com.yral.shared.rust.service.data

import com.yral.shared.rust.service.domain.UserInfoRepository
import com.yral.shared.rust.service.domain.models.FollowersPageResult
import com.yral.shared.rust.service.domain.models.FollowingPageResult
import com.yral.shared.rust.service.domain.models.ProfileUpdateDetails
import com.yral.shared.rust.service.domain.models.UserProfileDetails
import com.yral.shared.rust.service.domain.models.toDomain
import com.yral.shared.rust.service.domain.models.toFollowerPageResult
import com.yral.shared.rust.service.domain.models.toFollowingPageResult
import com.yral.shared.uniffi.generated.Principal

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
    ): UserProfileDetails =
        dataSource
            .getProfileDetailsV4(principal, targetPrincipal)
            .toDomain()

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

    override suspend fun updateProfileDetails(
        principal: Principal,
        details: ProfileUpdateDetails,
    ) = dataSource.updateProfileDetails(principal, details)
}
