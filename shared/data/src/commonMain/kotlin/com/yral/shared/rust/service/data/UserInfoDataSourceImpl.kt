package com.yral.shared.rust.service.data

import com.yral.shared.http.spacetime.SpacetimeDBRemoteDataSource
import com.yral.shared.http.spacetime.SpacetimeFollowersPage
import com.yral.shared.http.spacetime.SpacetimeFollowingPage
import com.yral.shared.rust.service.domain.models.ProfileUpdateDetailsV2
import com.yral.shared.rust.service.domain.models.UserProfileDetails
import com.yral.shared.rust.service.domain.models.toDomain

class UserInfoDataSourceImpl(
    private val spacetimeDBRemoteDataSource: SpacetimeDBRemoteDataSource,
) : UserInfoDataSource {
    override suspend fun followUser(
        principal: String,
        targetPrincipal: String,
    ): Unit =
        spacetimeDBRemoteDataSource.followUser(targetPrincipal)

    override suspend fun unfollowUser(
        principal: String,
        targetPrincipal: String,
    ): Unit =
        spacetimeDBRemoteDataSource.unfollowUser(targetPrincipal)

    override suspend fun getUserProfileDetailsV7(
        principal: String,
        targetPrincipal: String,
    ): UserProfileDetails =
        spacetimeDBRemoteDataSource.getUserProfileDetailsV7(targetPrincipal)
            ?.toDomain()
            ?: throw com.yral.shared.core.exceptions.YralException("User profile not found: $targetPrincipal")

    override suspend fun getUsersProfileDetails(
        principal: String,
        targetPrincipalIds: List<String>,
    ): List<UserProfileDetails> =
        spacetimeDBRemoteDataSource.getUsersProfileDetails(targetPrincipalIds).map { it.toDomain() }

    override suspend fun getFollowers(
        principal: String,
        targetPrincipal: String,
        cursorPrincipal: String?,
        limit: ULong,
        withCallerFollows: Boolean?,
    ): SpacetimeFollowersPage =
        spacetimeDBRemoteDataSource.getFollowers(targetPrincipal, limit, cursorPrincipal)

    override suspend fun getFollowing(
        principal: String,
        targetPrincipal: String,
        cursorPrincipal: String?,
        limit: ULong,
        withCallerFollows: Boolean?,
    ): SpacetimeFollowingPage =
        spacetimeDBRemoteDataSource.getFollowing(targetPrincipal, limit, cursorPrincipal)

    override suspend fun updateProfileDetailsV2(
        principal: String,
        details: ProfileUpdateDetailsV2,
    ) {
        spacetimeDBRemoteDataSource.updateProfileDetails(
            bio = details.bio?.takeUnless { it.isBlank() },
            websiteUrl = details.websiteUrl,
            profilePictureUrl = details.profilePictureUrl,
        )
    }

    override suspend fun acceptNewUserRegistrationV2(
        principal: String,
        newPrincipal: String,
        authenticated: Boolean,
        mainAccount: String?,
    ) {
        spacetimeDBRemoteDataSource.acceptNewUserRegistrationV2(
            newPrincipalText = newPrincipal,
            authenticated = authenticated,
            mainAccountText = mainAccount,
        )
    }
}
