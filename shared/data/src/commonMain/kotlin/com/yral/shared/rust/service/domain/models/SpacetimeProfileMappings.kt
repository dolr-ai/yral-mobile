package com.yral.shared.rust.service.domain.models

import com.yral.shared.http.spacetime.SpacetimeFollowersPage
import com.yral.shared.http.spacetime.SpacetimeFollowingPage
import com.yral.shared.http.spacetime.SpacetimeSubscriptionPlan
import com.yral.shared.http.spacetime.SpacetimeUserAccountType
import com.yral.shared.http.spacetime.SpacetimeUserProfileV7
import com.yral.shared.rust.service.utils.rewriteProfileImageUrl

/**
 * Map a `SpacetimeUserProfileV7` (from SpacetimeDB REST) to the domain
 * `UserProfileDetails` model.
 *
 * This replaces the old `UisUserProfileDetailsForFrontendV7.toDomain()` mapping
 * that went through the Rust FFI / IC canister.
 */
fun SpacetimeUserProfileV7.toDomain(): UserProfileDetails =
    UserProfileDetails(
        bio = bio.takeUnless { it.isBlank() },
        websiteUrl = websiteUrl.takeUnless { it.isBlank() },
        followingCount = followingCount,
        userFollowsCaller = userFollowsCaller,
        profilePictureUrl = profilePicture?.let { rewriteProfileImageUrl(it.url) },
        principalId = principalText,
        followersCount = followersCount,
        callerFollowsUser = callerFollowsUser,
        subscriptionPlan = subscriptionPlan.toDomain(),
        isAiInfluencer = isAiInfluencer,
        accountType = accountType.toDomain(),
    )

fun SpacetimeSubscriptionPlan.toDomain(): SubscriptionPlan =
    when (this) {
        is SpacetimeSubscriptionPlan.Free -> SubscriptionPlan.Free
        is SpacetimeSubscriptionPlan.Pro ->
            SubscriptionPlan.Pro(
                YralProSubscription(
                    freeVideoCreditsLeft = freeVideoCreditsLeft,
                    totalVideoCreditsAlloted = totalVideoCreditsAlloted,
                ),
            )
    }

fun SpacetimeUserAccountType.toDomain(): UserAccountType =
    when (this) {
        is SpacetimeUserAccountType.MainAccount -> UserAccountType.MainAccount(bots = bots)
        is SpacetimeUserAccountType.BotAccount -> UserAccountType.BotAccount(owner = owner)
    }

fun SpacetimeFollowersPage.toFollowerPageResult(usernames: Map<String, String>): FollowersPageResult =
    FollowersPageResult(
        nextCursor = nextCursor,
        followers =
            followers.map { follower ->
                FollowerItem(
                    callerFollows = follower.callerFollows,
                    profilePictureUrl = follower.profilePictureUrl.takeUnless { it.isBlank() },
                    principalId = follower.principalText,
                    username = usernames[follower.principalText],
                )
            },
        totalCount = totalCount,
    )

fun SpacetimeFollowingPage.toFollowingPageResult(usernames: Map<String, String>): FollowingPageResult =
    FollowingPageResult(
        nextCursor = nextCursor,
        following =
            following.map { follower ->
                FollowerItem(
                    callerFollows = follower.callerFollows,
                    profilePictureUrl = follower.profilePictureUrl.takeUnless { it.isBlank() },
                    principalId = follower.principalText,
                    username = usernames[follower.principalText],
                )
            },
        totalCount = totalCount,
    )