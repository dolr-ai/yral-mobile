package com.yral.shared.rust.service.domain.models

import com.yral.shared.uniffi.generated.UisUserProfileDetailsForFrontendV6
import com.yral.shared.uniffi.generated.UisUserProfileGlobalStats

fun UisUserProfileDetailsForFrontendV6.toDomain(): UserProfileDetails =
    UserProfileDetails(
        bio = bio,
        websiteUrl = websiteUrl,
        followingCount = followingCount,
        userFollowsCaller = userFollowsCaller,
        profilePictureUrl = profilePicture?.url,
        principalId = principalId,
        followersCount = followersCount,
        callerFollowsUser = callerFollowsUser,
        isAiInfluencer = isAiInfluencer,
    )

fun UisUserProfileGlobalStats.toDomain(): UserProfileGlobalStats =
    UserProfileGlobalStats(
        hotBetsReceived = hotBetsReceived,
        notBetsReceived = notBetsReceived,
    )
