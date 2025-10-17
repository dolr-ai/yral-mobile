package com.yral.shared.rust.service.domain.models

import com.yral.shared.uniffi.generated.UisUserProfileDetailsForFrontendV4
import com.yral.shared.uniffi.generated.UisUserProfileGlobalStats

fun UisUserProfileDetailsForFrontendV4.toDomain(): UserProfileDetails =
    UserProfileDetails(
        bio = bio,
        websiteUrl = websiteUrl,
        followingCount = followingCount,
        userFollowsCaller = userFollowsCaller,
        profilePictureUrl = profilePictureUrl,
        principalId = principalId,
        profileStats = profileStats.toDomain(),
        followersCount = followersCount,
        callerFollowsUser = callerFollowsUser,
    )

fun UisUserProfileGlobalStats.toDomain(): UserProfileGlobalStats =
    UserProfileGlobalStats(
        hotBetsReceived = hotBetsReceived,
        notBetsReceived = notBetsReceived,
    )
