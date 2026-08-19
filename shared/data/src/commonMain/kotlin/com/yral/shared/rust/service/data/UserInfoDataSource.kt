package com.yral.shared.rust.service.data

import com.yral.shared.http.spacetime.SpacetimeFollowersPage
import com.yral.shared.http.spacetime.SpacetimeFollowingPage
import com.yral.shared.rust.service.domain.models.ProfileUpdateDetailsV2
import com.yral.shared.rust.service.domain.models.UserProfileDetails

interface UserInfoDataSource {
    suspend fun followUser(
        principal: String,
        targetPrincipal: String,
    )

    suspend fun unfollowUser(
        principal: String,
        targetPrincipal: String,
    )

    suspend fun getUserProfileDetailsV7(
        principal: String,
        targetPrincipal: String,
    ): UserProfileDetails

    suspend fun getUsersProfileDetails(
        principal: String,
        targetPrincipalIds: List<String>,
    ): List<UserProfileDetails>

    suspend fun getFollowers(
        principal: String,
        targetPrincipal: String,
        cursorPrincipal: String?,
        limit: ULong,
        withCallerFollows: Boolean?,
    ): SpacetimeFollowersPage

    suspend fun getFollowing(
        principal: String,
        targetPrincipal: String,
        cursorPrincipal: String?,
        limit: ULong,
        withCallerFollows: Boolean?,
    ): SpacetimeFollowingPage

    suspend fun updateProfileDetailsV2(
        principal: String,
        details: ProfileUpdateDetailsV2,
    )

    suspend fun acceptNewUserRegistrationV2(
        principal: String,
        newPrincipal: String,
        authenticated: Boolean,
        mainAccount: String?,
    )
}
