package com.yral.shared.http.spacetime

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement

/**
 * SpacetimeDB `PostDetailsForFrontend` — the SATS positional array returned by
 * the `get_post_by_id` and related procedures via REST.
 *
 * SpacetimeDB serializes `SpacetimeType` structs as **positional JSON arrays**
 * via `SerdeWrapper(AlgebraicValue)`. Field order matches the Rust struct
 * definition in `apps/yral-database-spacetime/src/posts.rs`:
 *
 * ```rust
 * pub struct PostDetailsForFrontend {
 *     pub id: String,                    // [0]
 *     pub description: String,           // [1]
 *     pub hashtags: Vec<String>,         // [2]
 *     pub video_uid: String,             // [3]
 *     pub creator: Identity,             // [4] — ["0x<hex>"]
 *     pub creator_oauth_subject: String,  // [5]
 *     pub created_at: Timestamp,         // [6] — [<micros>]
 *     pub total_view_count: u64,         // [7]
 *     pub like_count: u64,               // [8]
 *     pub liked_by_me: bool,             // [9]
 *     pub status: PostStatus,            // [10] — [tag, payload]
 * }
 * ```
 *
 * `creator` is `Identity` serialized as `["0x<hex>"]` — we don't use it (we use
 * `creatorOauthSubject` instead). `createdAt` is `Timestamp` serialized as
 * `[<micros_since_epoch>]`. `status` is a sum type serialized as
 * `[variant_tag, payload]`.
 */
data class SpacetimePostDetails(
    val id: String,
    val description: String,
    val hashtags: List<String>,
    val videoUid: String,
    val creator: JsonElement?,
    val creatorOauthSubject: String,
    val createdAt: List<Long>,
    val totalViewCount: ULong,
    val likeCount: ULong,
    val likedByMe: Boolean,
    val status: SpacetimePostStatus,
) {
    companion object {
        private const val INDEX_ID = 0
        private const val INDEX_DESCRIPTION = 1
        private const val INDEX_HASHTAGS = 2
        private const val INDEX_VIDEO_UID = 3
        private const val INDEX_CREATOR = 4
        private const val INDEX_CREATOR_OAUTH_SUBJECT = 5
        private const val INDEX_CREATED_AT = 6
        private const val INDEX_TOTAL_VIEW_COUNT = 7
        private const val INDEX_LIKE_COUNT = 8
        private const val INDEX_LIKED_BY_ME = 9
        private const val INDEX_STATUS = 10

        /**
         * Decode a `PostDetailsForFrontend` from its SATS positional array
         * representation.
         */
        fun fromJsonArray(array: JsonArray): SpacetimePostDetails {
            val decoder = SpacetimePositionalDecoder(array)
            return SpacetimePostDetails(
                id = decoder.getString(INDEX_ID),
                description = decoder.getString(INDEX_DESCRIPTION),
                hashtags = decoder.getArrayOrNull(INDEX_HASHTAGS)?.let { parseStringVec(it) } ?: emptyList(),
                videoUid = decoder.getString(INDEX_VIDEO_UID),
                creator = decoder.getArrayOrNull(INDEX_CREATOR),
                creatorOauthSubject = decoder.getString(INDEX_CREATOR_OAUTH_SUBJECT),
                createdAt = decoder.getArrayOrNull(INDEX_CREATED_AT)?.let { parseLongVec(it) } ?: emptyList(),
                totalViewCount = decoder.getULong(INDEX_TOTAL_VIEW_COUNT),
                likeCount = decoder.getULong(INDEX_LIKE_COUNT),
                likedByMe = decoder.getBoolean(INDEX_LIKED_BY_ME),
                status = parsePostStatus(decoder.getArray(INDEX_STATUS)),
            )
        }
    }
}

/**
 * SpacetimeDB `PostStatus` enum — unit variants serialized as sum types:
 * `[tag, []]`. Tag matches the variant index in the Rust enum definition.
 *
 * ```rust
 * pub enum PostStatus {
 *     Uploaded,                 // 0
 *     Transcoding,              // 1
 *     CheckingExplicitness,     // 2
 *     BannedForExplicitness,    // 3
 *     ReadyToView,              // 4
 *     BannedDueToUserReporting, // 5
 *     Deleted,                  // 6
 *     Draft,                    // 7
 * }
 * ```
 */
enum class SpacetimePostStatus {
    Uploaded,
    Transcoding,
    CheckingExplicitness,
    BannedForExplicitness,
    ReadyToView,
    BannedDueToUserReporting,
    Deleted,
    Draft,
}

/**
 * Parse a `PostStatus` from its SATS sum-type encoding `[tag, payload]`.
 */
internal fun parsePostStatus(array: JsonArray): SpacetimePostStatus {
    val variant = SumVariant.fromArray(array)
    return SpacetimePostStatus.entries[variant.tag]
}
