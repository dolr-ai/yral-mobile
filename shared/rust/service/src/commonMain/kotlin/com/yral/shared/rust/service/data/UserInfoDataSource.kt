package com.yral.shared.rust.service.data

import com.yral.shared.rust.service.domain.models.ProfileUpdateDetailsV2
import com.yral.shared.uniffi.generated.Principal
import com.yral.shared.uniffi.generated.UisFollowersResponse
import com.yral.shared.uniffi.generated.UisFollowingResponse
import com.yral.shared.uniffi.generated.UisUserProfileDetailsForFrontendV6

interface UserInfoDataSource {
    suspend fun followUser(
        principal: Principal,
        targetPrincipal: Principal,
    )

    suspend fun unfollowUser(
        principal: Principal,
        targetPrincipal: Principal,
    )

    suspend fun getUserProfileDetailsV6(
        principal: Principal,
        targetPrincipal: Principal,
    ): UisUserProfileDetailsForFrontendV6

    suspend fun getFollowers(
        principal: Principal,
        targetPrincipal: Principal,
        cursorPrincipal: Principal?,
        limit: ULong,
        withCallerFollows: Boolean?,
    ): UisFollowersResponse

    suspend fun getFollowing(
        principal: Principal,
        targetPrincipal: Principal,
        cursorPrincipal: Principal?,
        limit: ULong,
        withCallerFollows: Boolean?,
    ): UisFollowingResponse

    suspend fun updateProfileDetailsV2(
        principal: Principal,
        details: ProfileUpdateDetailsV2,
    )
}
