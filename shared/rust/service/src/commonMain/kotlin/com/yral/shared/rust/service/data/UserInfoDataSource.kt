package com.yral.shared.rust.service.data

import com.yral.shared.rust.service.domain.models.ProfileUpdateDetailsV2
import com.yral.shared.uniffi.generated.Principal
import com.yral.shared.uniffi.generated.UisFollowersResponse
import com.yral.shared.uniffi.generated.UisFollowingResponse
import com.yral.shared.uniffi.generated.UisUserProfileDetailsForFrontendV7

interface UserInfoDataSource {
    suspend fun followUser(
        principal: Principal,
        targetPrincipal: Principal,
    )

    suspend fun unfollowUser(
        principal: Principal,
        targetPrincipal: Principal,
    )

    suspend fun getUserProfileDetailsV7(
        principal: Principal,
        targetPrincipal: Principal,
    ): UisUserProfileDetailsForFrontendV7

    suspend fun getUsersProfileDetails(
        principal: Principal,
        targetPrincipalIds: List<String>,
    ): List<UisUserProfileDetailsForFrontendV7>

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

    suspend fun acceptNewUserRegistrationV2(
        principal: Principal,
        newPrincipal: Principal,
        authenticated: Boolean,
        mainAccount: Principal?,
    )
}
