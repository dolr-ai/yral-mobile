package com.yral.shared.rust.service.data

import com.yral.shared.rust.service.domain.models.ProfileUpdateDetails
import com.yral.shared.rust.service.services.UserInfoServiceFactory
import com.yral.shared.uniffi.generated.Principal
import com.yral.shared.uniffi.generated.UisFollowersResponse
import com.yral.shared.uniffi.generated.UisFollowingResponse
import com.yral.shared.uniffi.generated.UisProfileUpdateDetails
import com.yral.shared.uniffi.generated.UisUserProfileDetailsForFrontendV4

class UserInfoDataSourceImpl(
    private val userInfoServiceFactory: UserInfoServiceFactory,
) : UserInfoDataSource {
    override suspend fun followUser(
        principal: Principal,
        targetPrincipal: Principal,
    ): Unit =
        userInfoServiceFactory
            .service(principal)
            .followUser(targetPrincipal)

    override suspend fun unfollowUser(
        principal: Principal,
        targetPrincipal: Principal,
    ): Unit =
        userInfoServiceFactory
            .service(principal)
            .unfollowUser(targetPrincipal)

    override suspend fun getProfileDetailsV4(
        principal: Principal,
        targetPrincipal: Principal,
    ): UisUserProfileDetailsForFrontendV4 =
        userInfoServiceFactory
            .service(principal)
            .getProfileDetailsV4(targetPrincipal)

    override suspend fun getFollowers(
        principal: Principal,
        targetPrincipal: Principal,
        cursorPrincipal: Principal?,
        limit: ULong,
        withCallerFollows: Boolean?,
    ): UisFollowersResponse =
        userInfoServiceFactory
            .service(principal)
            .getFollowers(
                principalText = targetPrincipal,
                cursorPrincipalText = cursorPrincipal,
                limit = limit,
                withCallerFollows = withCallerFollows,
            )

    override suspend fun getFollowing(
        principal: Principal,
        targetPrincipal: Principal,
        cursorPrincipal: Principal?,
        limit: ULong,
        withCallerFollows: Boolean?,
    ): UisFollowingResponse =
        userInfoServiceFactory
            .service(principal)
            .getFollowing(
                principalText = targetPrincipal,
                cursorPrincipalText = cursorPrincipal,
                limit = limit,
                withCallerFollows = withCallerFollows,
            )

    override suspend fun updateProfileDetails(
        principal: Principal,
        details: ProfileUpdateDetails,
    ) {
        userInfoServiceFactory
            .service(principal)
            .updateProfileDetails(
                UisProfileUpdateDetails(
                    bio = details.bio?.takeUnless { it.isBlank() },
                    websiteUrl = details.websiteUrl,
                    profilePictureUrl = details.profilePictureUrl,
                ),
            )
    }
}
