package com.yral.shared.http.spacetime

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// ─────────────────────────────────────────────────────────────────────────
// User profile models
// ─────────────────────────────────────────────────────────────────────────

/**
 * SpacetimeDB `UserProfileDetailsV7` — returned by `get_user_profile_details_v7`.
 *
 * SpacetimeDB serializes fields in **camelCase**.
 * `callerFollowsUser` and `userFollowsCaller` are `null` when the caller
 * is viewing their own profile (self).
 */
@Serializable
data class SpacetimeUserProfileV7(
    @SerialName("principalText") val principalText: String,
    @SerialName("profilePicture") val profilePicture: SpacetimeProfilePictureData? = null,
    val bio: String = "",
    @SerialName("websiteUrl") val websiteUrl: String = "",
    @SerialName("followersCount") val followersCount: ULong = 0u,
    @SerialName("followingCount") val followingCount: ULong = 0u,
    @SerialName("callerFollowsUser") val callerFollowsUser: Boolean? = null,
    @SerialName("userFollowsCaller") val userFollowsCaller: Boolean? = null,
    @SerialName("subscriptionPlan") val subscriptionPlan: SpacetimeSubscriptionPlan,
    @SerialName("isAiInfluencer") val isAiInfluencer: Boolean = false,
    @SerialName("accountType") val accountType: SpacetimeUserAccountType,
)

@Serializable
data class SpacetimeProfilePictureData(
    val url: String,
    @SerialName("nsfwInfo") val nsfwInfo: SpacetimeNsfwInfo = SpacetimeNsfwInfo(),
)

@Serializable
data class SpacetimeNsfwInfo(
    @SerialName("isNsfw") val isNsfw: Boolean = false,
    @SerialName("nsfwEc") val nsfwEc: String = "",
    @SerialName("nsfwGore") val nsfwGore: String = "",
    @SerialName("csamDetected") val csamDetected: Boolean = false,
)

/**
 * SpacetimeDB `SubscriptionPlan` enum — externally tagged.
 * `"Free"` for the unit variant; `{"Pro": {...}}` for the newtype variant.
 */
@Serializable
sealed class SpacetimeSubscriptionPlan {
    @Serializable
    @SerialName("Free")
    data object Free : SpacetimeSubscriptionPlan()

    @Serializable
    @SerialName("Pro")
    data class Pro(
        @SerialName("freeVideoCreditsLeft") val freeVideoCreditsLeft: UInt = 0u,
        @SerialName("totalVideoCreditsAlloted") val totalVideoCreditsAlloted: UInt = 0u,
    ) : SpacetimeSubscriptionPlan()
}

/**
 * SpacetimeDB `UserAccountType` enum — externally tagged with struct variants.
 * `{"MainAccount": {"bots": [...]}}` / `{"BotAccount": {"owner": "..."}}`.
 */
@Serializable
sealed class SpacetimeUserAccountType {
    @Serializable
    @SerialName("MainAccount")
    data class MainAccount(
        val bots: List<String> = emptyList(),
    ) : SpacetimeUserAccountType()

    @Serializable
    @SerialName("BotAccount")
    data class BotAccount(
        val owner: String,
    ) : SpacetimeUserAccountType()
}

// ─────────────────────────────────────────────────────────────────────────
// Followers / Following models
// ─────────────────────────────────────────────────────────────────────────

@Serializable
data class SpacetimeFollowersPage(
    val followers: List<SpacetimeFollowerItem> = emptyList(),
    @SerialName("totalCount") val totalCount: ULong = 0u,
    @SerialName("nextCursor") val nextCursor: String? = null,
)

@Serializable
data class SpacetimeFollowerItem(
    @SerialName("principalText") val principalText: String,
    @SerialName("callerFollows") val callerFollows: Boolean = false,
    @SerialName("profilePictureUrl") val profilePictureUrl: String = "",
)

@Serializable
data class SpacetimeFollowingPage(
    val following: List<SpacetimeFollowingItem> = emptyList(),
    @SerialName("totalCount") val totalCount: ULong = 0u,
    @SerialName("nextCursor") val nextCursor: String? = null,
)

@Serializable
data class SpacetimeFollowingItem(
    @SerialName("principalText") val principalText: String,
    @SerialName("callerFollows") val callerFollows: Boolean = false,
    @SerialName("profilePictureUrl") val profilePictureUrl: String = "",
)

// ─────────────────────────────────────────────────────────────────────────
// Post list models
// ─────────────────────────────────────────────────────────────────────────

/**
 * SpacetimeDB `PostListOffset` — returned by `get_posts_of_user_by_principal`
 * and `get_draft_posts_of_user_by_principal`. Offset-based pagination (no cursor).
 */
@Serializable
data class SpacetimePostListOffset(
    val posts: List<SpacetimePostDetails> = emptyList(),
)