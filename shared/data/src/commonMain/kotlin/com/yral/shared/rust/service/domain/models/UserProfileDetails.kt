package com.yral.shared.rust.service.domain.models

data class UserProfileDetails(
    val bio: String?,
    val websiteUrl: String?,
    val followingCount: ULong,
    val userFollowsCaller: Boolean?,
    val profilePictureUrl: String?,
    val principalId: String,
    val followersCount: ULong,
    val callerFollowsUser: Boolean?,
    val subscriptionPlan: SubscriptionPlan,
    val isAiInfluencer: Boolean?,
    val accountType: UserAccountType,
)
