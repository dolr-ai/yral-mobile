package com.yral.shared.rust.service.domain.models

import com.yral.shared.uniffi.generated.UisSubscriptionPlan
import com.yral.shared.uniffi.generated.UisUserAccountType
import com.yral.shared.uniffi.generated.UisUserProfileDetailsForFrontendV7
import com.yral.shared.uniffi.generated.UisUserProfileGlobalStats
import com.yral.shared.uniffi.generated.UisYralProSubscription

fun UisUserProfileDetailsForFrontendV7.toDomain(): UserProfileDetails =
    UserProfileDetails(
        bio = bio,
        websiteUrl = websiteUrl,
        followingCount = followingCount,
        userFollowsCaller = userFollowsCaller,
        profilePictureUrl = profilePicture?.url,
        principalId = principalId,
        followersCount = followersCount,
        callerFollowsUser = callerFollowsUser,
        subscriptionPlan = subscriptionPlan.toDomain(),
        isAiInfluencer = isAiInfluencer,
        accountType = accountType.toDomain(),
    )

fun UisSubscriptionPlan.toDomain(): SubscriptionPlan =
    when (this) {
        is UisSubscriptionPlan.Free -> SubscriptionPlan.Free
        is UisSubscriptionPlan.Pro -> SubscriptionPlan.Pro(v1.toDomain())
    }

fun UisYralProSubscription.toDomain(): YralProSubscription =
    YralProSubscription(
        freeVideoCreditsLeft = freeVideoCreditsLeft,
        totalVideoCreditsAlloted = totalVideoCreditsAlloted,
    )

fun UisUserAccountType.toDomain(): UserAccountType =
    when (this) {
        is UisUserAccountType.MainAccount -> UserAccountType.MainAccount(bots = bots)
        is UisUserAccountType.BotAccount -> UserAccountType.BotAccount(owner = owner)
    }

fun UisUserProfileGlobalStats.toDomain(): UserProfileGlobalStats =
    UserProfileGlobalStats(
        hotBetsReceived = hotBetsReceived,
        notBetsReceived = notBetsReceived,
    )
