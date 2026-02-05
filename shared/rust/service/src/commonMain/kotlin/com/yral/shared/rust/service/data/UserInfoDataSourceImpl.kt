package com.yral.shared.rust.service.data

import com.yral.shared.rust.service.domain.models.ProfileUpdateDetailsV2
import com.yral.shared.rust.service.services.UserInfoServiceFactory
import com.yral.shared.uniffi.generated.Principal
import com.yral.shared.uniffi.generated.UisFollowersResponse
import com.yral.shared.uniffi.generated.UisFollowingResponse
import com.yral.shared.uniffi.generated.UisNsfwInfo
import com.yral.shared.uniffi.generated.UisProfilePictureData
import com.yral.shared.uniffi.generated.UisProfileUpdateDetailsV2
import com.yral.shared.uniffi.generated.UisUserProfileDetailsForFrontendV7

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

    override suspend fun getUserProfileDetailsV7(
        principal: Principal,
        targetPrincipal: Principal,
    ): UisUserProfileDetailsForFrontendV7 =
        userInfoServiceFactory
            .service(principal)
            .getUserProfileDetailsV7(targetPrincipal)

    override suspend fun getUsersProfileDetails(
        principal: Principal,
        targetPrincipalIds: List<String>,
    ): List<UisUserProfileDetailsForFrontendV7> =
        userInfoServiceFactory
            .service(principal)
            .getUsersProfileDetails(targetPrincipalIds)

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

    override suspend fun updateProfileDetailsV2(
        principal: Principal,
        details: ProfileUpdateDetailsV2,
    ) {
        userInfoServiceFactory
            .service(principal)
            .updateProfileDetailsV2(
                UisProfileUpdateDetailsV2(
                    bio = details.bio?.takeUnless { it.isBlank() },
                    websiteUrl = details.websiteUrl,
                    profilePicture =
                        details.profilePictureUrl
                            ?.takeUnless { it.isBlank() }
                            ?.let { url ->
                                UisProfilePictureData(
                                    url = url,
                                    nsfwInfo =
                                        UisNsfwInfo(
                                            isNsfw = false,
                                            nsfwEc = "",
                                            nsfwGore = "",
                                            csamDetected = false,
                                        ),
                                )
                            },
                ),
            )
    }

    override suspend fun acceptNewUserRegistrationV2(
        principal: Principal,
        newPrincipal: Principal,
        authenticated: Boolean,
        mainAccount: Principal?,
    ) {
        userInfoServiceFactory
            .service(principal)
            .acceptNewUserRegistrationV2(
                newPrincipalText = newPrincipal,
                authenticated = authenticated,
                mainAccountText = mainAccount,
            )
    }
}
