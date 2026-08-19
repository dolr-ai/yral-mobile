package com.yral.shared.rust.service.domain

import com.yral.shared.rust.service.domain.models.FollowersPageResult
import com.yral.shared.rust.service.domain.models.FollowingPageResult
import com.yral.shared.rust.service.domain.models.ProfileUpdateDetailsV2
import com.yral.shared.rust.service.domain.models.UserProfileDetails

interface UserInfoRepository {
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
    ): Map<String, UserProfileDetails>

    suspend fun getFollowers(
        principal: String,
        targetPrincipal: String,
        cursorPrincipal: String?,
        limit: ULong,
        withCallerFollows: Boolean?,
    ): FollowersPageResult

    suspend fun getFollowing(
        principal: String,
        targetPrincipal: String,
        cursorPrincipal: String?,
        limit: ULong,
        withCallerFollows: Boolean?,
    ): FollowingPageResult

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
