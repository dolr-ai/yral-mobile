package com.yral.shared.rust.service.data

import com.yral.shared.http.spacetime.SpacetimeFollowersPage
import com.yral.shared.http.spacetime.SpacetimeFollowingPage
import com.yral.shared.rust.service.domain.models.ProfileUpdateDetailsV2
import com.yral.shared.rust.service.domain.models.UserProfileDetails
import com.yral.shared.uniffi.generated.Principal

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
    ): UserProfileDetails

    suspend fun getUsersProfileDetails(
        principal: Principal,
        targetPrincipalIds: List<String>,
    ): List<UserProfileDetails>

    suspend fun getFollowers(
        principal: Principal,
        targetPrincipal: Principal,
        cursorPrincipal: Principal?,
        limit: ULong,
        withCallerFollows: Boolean?,
    ): SpacetimeFollowersPage

    suspend fun getFollowing(
        principal: Principal,
        targetPrincipal: Principal,
        cursorPrincipal: Principal?,
        limit: ULong,
        withCallerFollows: Boolean?,
    ): SpacetimeFollowingPage

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
