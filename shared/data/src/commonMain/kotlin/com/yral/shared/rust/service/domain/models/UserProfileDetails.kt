package com.yral.shared.rust.service.domain.models

import com.yral.shared.uniffi.generated.Principal

data class UserProfileDetails(
    val bio: String?,
    val websiteUrl: String?,
    val followingCount: ULong,
    val userFollowsCaller: Boolean?,
    val profilePictureUrl: String?,
    val principalId: Principal,
    val followersCount: ULong,
    val callerFollowsUser: Boolean?,
    val subscriptionPlan: SubscriptionPlan,
    val isAiInfluencer: Boolean?,
    val accountType: UserAccountType,
)
