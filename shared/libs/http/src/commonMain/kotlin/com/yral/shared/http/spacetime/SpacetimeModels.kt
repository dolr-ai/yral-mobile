package com.yral.shared.http.spacetime

import kotlinx.serialization.json.JsonArray

// ─────────────────────────────────────────────────────────────────────────
// User profile models
// ─────────────────────────────────────────────────────────────────────────

/**
 * SpacetimeDB `UserProfileDetails` — returned by `get_user_profile_details`.
 *
 * SpacetimeDB serializes `SpacetimeType` structs as **positional JSON arrays**.
 * Field order matches the Rust struct definition in
 * `apps/yral-database-spacetime/src/user_info.rs`:
 *
 * ```rust
 * pub struct UserProfileDetailsV7 {
 *     pub oauth_subject: String,                  // [0]
 *     pub profile_picture: Option<ProfilePictureData>, // [1] — [0, payload] or [1, []]
 *     pub bio: String,                            // [2]
 *     pub website_url: String,                    // [3]
 *     pub followers_count: u64,                   // [4]
 *     pub following_count: u64,                   // [5]
 *     pub caller_follows_user: Option<bool>,      // [6] — [0, bool] or [1, []]
 *     pub user_follows_caller: Option<bool>,      // [7] — [0, bool] or [1, []]
 *     pub subscription_plan: SubscriptionPlan,    // [8] — [tag, payload]
 *     pub is_ai_influencer: bool,                 // [9]
 *     pub account_type: UserAccountType,          // [10] — [tag, payload]
 * }
 * ```
 *
 * `callerFollowsUser` and `userFollowsCaller` are `null` when the caller
 * is viewing their own profile (self — SpacetimeDB returns `None` for both).
 */
data class SpacetimeUserProfileV7(
    val oauthSubject: String,
    val profilePicture: SpacetimeProfilePictureData?,
    val bio: String,
    val websiteUrl: String,
    val followersCount: ULong,
    val followingCount: ULong,
    val callerFollowsUser: Boolean?,
    val userFollowsCaller: Boolean?,
    val subscriptionPlan: SpacetimeSubscriptionPlan,
    val isAiInfluencer: Boolean,
    val accountType: SpacetimeUserAccountType,
) {
    companion object {
        fun fromJsonArray(array: JsonArray): SpacetimeUserProfileV7 {
            val decoder = SpacetimePositionalDecoder(array)
            return SpacetimeUserProfileV7(
                oauthSubject = decoder.getString(0),
                profilePicture = decoder.getArrayOrNull(1)?.let { parseOptionArray(it) }
                    ?.let { SpacetimeProfilePictureData.fromJsonArray(it) },
                bio = decoder.getString(2),
                websiteUrl = decoder.getString(3),
                followersCount = decoder.getULong(4),
                followingCount = decoder.getULong(5),
                callerFollowsUser = decoder.getArrayOrNull(6)?.let { parseOptionBool(it) },
                userFollowsCaller = decoder.getArrayOrNull(7)?.let { parseOptionBool(it) },
                subscriptionPlan = parseSubscriptionPlan(decoder.getArray(8)),
                isAiInfluencer = decoder.getBoolean(9),
                accountType = parseUserAccountType(decoder.getArray(10)),
            )
        }
    }
}

/**
 * SpacetimeDB `ProfilePictureData` — positional array:
 * `[url, nsfw_info]` where `nsfw_info` is `[is_nsfw, nsfw_ec, nsfw_gore, csam_detected]`.
 *
 * ```rust
 * pub struct ProfilePictureData {   // [0]     // [1]
 *     pub url: String,              // [0][0]
 *     pub nsfw_info: NSFWInfo,      // [0][1]
 * }
 * pub struct NSFWInfo {
 *     pub is_nsfw: bool,            // [1][0]
 *     pub nsfw_ec: String,          // [1][1]
 *     pub nsfw_gore: String,        // [1][2]
 *     pub csam_detected: bool,      // [1][3]
 * }
 * ```
 */
data class SpacetimeProfilePictureData(
    val url: String,
    val nsfwInfo: SpacetimeNsfwInfo,
) {
    companion object {
        fun fromJsonArray(array: JsonArray): SpacetimeProfilePictureData {
            val decoder = SpacetimePositionalDecoder(array)
            return SpacetimeProfilePictureData(
                url = decoder.getString(0),
                nsfwInfo = SpacetimeNsfwInfo.fromJsonArray(decoder.getArray(1)),
            )
        }
    }
}

data class SpacetimeNsfwInfo(
    val isNsfw: Boolean,
    val nsfwEc: String,
    val nsfwGore: String,
    val csamDetected: Boolean,
) {
    companion object {
        fun fromJsonArray(array: JsonArray): SpacetimeNsfwInfo {
            val decoder = SpacetimePositionalDecoder(array)
            return SpacetimeNsfwInfo(
                isNsfw = decoder.getBoolean(0),
                nsfwEc = decoder.getString(1),
                nsfwGore = decoder.getString(2),
                csamDetected = decoder.getBoolean(3),
            )
        }
    }
}

/**
 * SpacetimeDB `SubscriptionPlan` enum — sum type `[tag, payload]`.
 * - `Free` → `[0, []]` (unit variant)
 * - `Pro(YralProSubscription)` → `[1, [free_video_credits_left, total_video_credits_alloted]]`
 */
sealed class SpacetimeSubscriptionPlan {
    data object Free : SpacetimeSubscriptionPlan()

    data class Pro(
        val freeVideoCreditsLeft: UInt,
        val totalVideoCreditsAlloted: UInt,
    ) : SpacetimeSubscriptionPlan()
}

internal fun parseSubscriptionPlan(array: JsonArray): SpacetimeSubscriptionPlan {
    val variant = SumVariant.fromArray(array)
    return when (variant.tag) {
        0 -> SpacetimeSubscriptionPlan.Free
        1 -> {
            val decoder = SpacetimePositionalDecoder(variant.payload)
            SpacetimeSubscriptionPlan.Pro(
                freeVideoCreditsLeft = decoder.getUInt(0),
                totalVideoCreditsAlloted = decoder.getUInt(1),
            )
        }
        else -> throw IllegalArgumentException("Unknown SubscriptionPlan variant tag: ${variant.tag}")
    }
}

/**
 * SpacetimeDB `UserAccountType` enum — sum type `[tag, payload]`.
 * - `MainAccount { bots: Vec<String> }` → `[0, [[bot0, bot1, ...]]]`
 * - `BotAccount { owner: String }` → `[1, [owner_string]]`
 */
sealed class SpacetimeUserAccountType {
    data class MainAccount(
        val bots: List<String>,
    ) : SpacetimeUserAccountType()

    data class BotAccount(
        val owner: String,
    ) : SpacetimeUserAccountType()
}

internal fun parseUserAccountType(array: JsonArray): SpacetimeUserAccountType {
    val variant = SumVariant.fromArray(array)
    return when (variant.tag) {
        0 -> {
            val decoder = SpacetimePositionalDecoder(variant.payload)
            SpacetimeUserAccountType.MainAccount(
                bots = decoder.getArrayOrNull(0)?.let { parseStringVec(it) } ?: emptyList(),
            )
        }
        1 -> {
            val decoder = SpacetimePositionalDecoder(variant.payload)
            SpacetimeUserAccountType.BotAccount(
                owner = decoder.getString(0),
            )
        }
        else -> throw IllegalArgumentException("Unknown UserAccountType variant tag: ${variant.tag}")
    }
}

/**
 * Parse a SATS `Option<bool>` from its sum-type encoding.
 * - `Some(true)` → `[0, true]`
 * - `Some(false)` → `[0, false]`
 * - `None` → `[1, []]`
 */
internal fun parseOptionBool(array: JsonArray): Boolean? {
    val payload = parseOptionArray(array) ?: return null
    val decoder = SpacetimePositionalDecoder(payload)
    return decoder.getBoolean(0)
}

// ─────────────────────────────────────────────────────────────────────────
// Followers / Following models
// ─────────────────────────────────────────────────────────────────────────

/**
 * SpacetimeDB `FollowersPage` — positional array:
 *
 * ```rust
 * pub struct FollowersPage {
 *     pub followers: Vec<FollowerItem>,    // [0]
 *     pub total_count: u64,                 // [1]
 *     pub next_cursor: Option<String>,      // [2]
 * }
 * pub struct FollowerItem {
 *     pub oauth_subject: String,           // [0]
 *     pub caller_follows: bool,            // [1]
 *     pub profile_picture_url: String,     // [2]
 * }
 * ```
 */
data class SpacetimeFollowersPage(
    val followers: List<SpacetimeFollowerItem>,
    val totalCount: ULong,
    val nextCursor: String?,
) {
    companion object {
        fun fromJsonArray(array: JsonArray): SpacetimeFollowersPage {
            val decoder = SpacetimePositionalDecoder(array)
            return SpacetimeFollowersPage(
                followers = decoder.getArrayOrNull(0)?.map { SpacetimeFollowerItem.fromJsonArray(it as JsonArray) } ?: emptyList(),
                totalCount = decoder.getULong(1),
                nextCursor = decoder.getArrayOrNull(2)?.let { parseOptionString(it) },
            )
        }
    }
}

data class SpacetimeFollowerItem(
    val oauthSubject: String,
    val callerFollows: Boolean,
    val profilePictureUrl: String,
) {
    companion object {
        fun fromJsonArray(array: JsonArray): SpacetimeFollowerItem {
            val decoder = SpacetimePositionalDecoder(array)
            return SpacetimeFollowerItem(
                oauthSubject = decoder.getString(0),
                callerFollows = decoder.getBoolean(1),
                profilePictureUrl = decoder.getString(2),
            )
        }
    }
}

/**
 * SpacetimeDB `FollowingPage` — positional array:
 *
 * ```rust
 * pub struct FollowingPage {
 *     pub following: Vec<FollowingItem>,   // [0]
 *     pub total_count: u64,                 // [1]
 *     pub next_cursor: Option<String>,      // [2]
 * }
 * pub struct FollowingItem {
 *     pub oauth_subject: String,           // [0]
 *     pub caller_follows: bool,            // [1]
 *     pub profile_picture_url: String,     // [2]
 * }
 * ```
 */
data class SpacetimeFollowingPage(
    val following: List<SpacetimeFollowingItem>,
    val totalCount: ULong,
    val nextCursor: String?,
) {
    companion object {
        fun fromJsonArray(array: JsonArray): SpacetimeFollowingPage {
            val decoder = SpacetimePositionalDecoder(array)
            return SpacetimeFollowingPage(
                following = decoder.getArrayOrNull(0)?.map { SpacetimeFollowingItem.fromJsonArray(it as JsonArray) } ?: emptyList(),
                totalCount = decoder.getULong(1),
                nextCursor = decoder.getArrayOrNull(2)?.let { parseOptionString(it) },
            )
        }
    }
}

data class SpacetimeFollowingItem(
    val oauthSubject: String,
    val callerFollows: Boolean,
    val profilePictureUrl: String,
) {
    companion object {
        fun fromJsonArray(array: JsonArray): SpacetimeFollowingItem {
            val decoder = SpacetimePositionalDecoder(array)
            return SpacetimeFollowingItem(
                oauthSubject = decoder.getString(0),
                callerFollows = decoder.getBoolean(1),
                profilePictureUrl = decoder.getString(2),
            )
        }
    }
}

/**
 * Parse a SATS `Option<String>` from its sum-type encoding.
 * - `Some(s)` → `[0, "string"]`
 * - `None` → `[1, []]`
 */
internal fun parseOptionString(array: JsonArray): String? {
    val payload = parseOptionArray(array) ?: return null
    val decoder = SpacetimePositionalDecoder(payload)
    return decoder.getString(0)
}

// ─────────────────────────────────────────────────────────────────────────
// Post list models
// ─────────────────────────────────────────────────────────────────────────

/**
 * SpacetimeDB `PostListOffset` — returned by `get_posts_of_user_by_principal`
 * and `get_draft_posts_of_user_by_principal`. Offset-based pagination (no cursor).
 *
 * ```rust
 * pub struct PostListOffset {
 *     pub posts: Vec<PostDetailsForFrontend>,  // [0]
 * }
 * ```
 */
data class SpacetimePostListOffset(
    val posts: List<SpacetimePostDetails>,
) {
    companion object {
        fun fromJsonArray(array: JsonArray): SpacetimePostListOffset {
            val decoder = SpacetimePositionalDecoder(array)
            return SpacetimePostListOffset(
                posts = decoder.getArrayOrNull(0)?.map { SpacetimePostDetails.fromJsonArray(it as JsonArray) } ?: emptyList(),
            )
        }
    }
}
