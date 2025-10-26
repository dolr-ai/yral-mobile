package com.yral.shared.rust.service.domain

import com.yral.shared.rust.service.domain.models.FollowersPageResult
import com.yral.shared.rust.service.domain.models.FollowingPageResult
import com.yral.shared.rust.service.domain.models.ProfileUpdateDetails
import com.yral.shared.rust.service.domain.models.UserProfileDetails
import com.yral.shared.uniffi.generated.Principal

interface UserInfoRepository {
    suspend fun followUser(
        principal: Principal,
        targetPrincipal: Principal,
    )

    suspend fun unfollowUser(
        principal: Principal,
        targetPrincipal: Principal,
    )

    suspend fun getProfileDetailsV4(
        principal: Principal,
        targetPrincipal: Principal,
    ): UserProfileDetails

    suspend fun getFollowers(
        principal: Principal,
        targetPrincipal: Principal,
        cursorPrincipal: Principal?,
        limit: ULong,
        withCallerFollows: Boolean?,
    ): FollowersPageResult

    suspend fun getFollowing(
        principal: Principal,
        targetPrincipal: Principal,
        cursorPrincipal: Principal?,
        limit: ULong,
        withCallerFollows: Boolean?,
    ): FollowingPageResult

    suspend fun updateProfileDetails(
        principal: Principal,
        details: ProfileUpdateDetails,
    )
}
