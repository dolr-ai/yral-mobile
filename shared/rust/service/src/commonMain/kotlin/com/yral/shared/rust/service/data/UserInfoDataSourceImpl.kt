package com.yral.shared.rust.service.data

import com.yral.shared.http.spacetime.SpacetimeDBRemoteDataSource
import com.yral.shared.http.spacetime.SpacetimeFollowersPage
import com.yral.shared.http.spacetime.SpacetimeFollowingPage
import com.yral.shared.rust.service.domain.models.ProfileUpdateDetailsV2
import com.yral.shared.rust.service.domain.models.UserProfileDetails
import com.yral.shared.rust.service.domain.models.toDomain
import com.yral.shared.rust.service.services.UserInfoServiceFactory
import com.yral.shared.uniffi.generated.Principal

class UserInfoDataSourceImpl(
    private val userInfoServiceFactory: UserInfoServiceFactory,
    private val spacetimeDBRemoteDataSource: SpacetimeDBRemoteDataSource,
) : UserInfoDataSource {
    override suspend fun followUser(
        principal: Principal,
        targetPrincipal: Principal,
    ): Unit =
        spacetimeDBRemoteDataSource.followUser(targetPrincipal)

    override suspend fun unfollowUser(
        principal: Principal,
        targetPrincipal: Principal,
    ): Unit =
        spacetimeDBRemoteDataSource.unfollowUser(targetPrincipal)

    override suspend fun getUserProfileDetailsV7(
        principal: Principal,
        targetPrincipal: Principal,
    ): UserProfileDetails =
        spacetimeDBRemoteDataSource.getUserProfileDetailsV7(targetPrincipal)
            ?.toDomain()
            ?: throw com.yral.shared.core.exceptions.YralException("User profile not found: $targetPrincipal")

    override suspend fun getUsersProfileDetails(
        principal: Principal,
        targetPrincipalIds: List<String>,
    ): List<UserProfileDetails> =
        spacetimeDBRemoteDataSource.getUsersProfileDetails(targetPrincipalIds).map { it.toDomain() }

    override suspend fun getFollowers(
        principal: Principal,
        targetPrincipal: Principal,
        cursorPrincipal: Principal?,
        limit: ULong,
        withCallerFollows: Boolean?,
    ): SpacetimeFollowersPage =
        spacetimeDBRemoteDataSource.getFollowers(targetPrincipal, limit, cursorPrincipal)

    override suspend fun getFollowing(
        principal: Principal,
        targetPrincipal: Principal,
        cursorPrincipal: Principal?,
        limit: ULong,
        withCallerFollows: Boolean?,
    ): SpacetimeFollowingPage =
        spacetimeDBRemoteDataSource.getFollowing(targetPrincipal, limit, cursorPrincipal)

    override suspend fun updateProfileDetailsV2(
        principal: Principal,
        details: ProfileUpdateDetailsV2,
    ) {
        spacetimeDBRemoteDataSource.updateProfileDetails(
            bio = details.bio?.takeUnless { it.isBlank() },
            websiteUrl = details.websiteUrl,
            profilePictureUrl = details.profilePictureUrl,
        )
    }

    override suspend fun acceptNewUserRegistrationV2(
        principal: Principal,
        newPrincipal: Principal,
        authenticated: Boolean,
        mainAccount: Principal?,
    ) {
        spacetimeDBRemoteDataSource.acceptNewUserRegistrationV2(
            newPrincipalText = newPrincipal,
            authenticated = authenticated,
            mainAccountText = mainAccount,
        )
    }
}
