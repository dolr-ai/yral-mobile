package com.yral.shared.http.spacetime

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * SpacetimeDB `PostDetailsForFrontend` — the JSON shape returned by the
 * `get_post_by_id` procedure via REST.
 *
 * SpacetimeDB serializes fields in **camelCase** (the `SpacetimeType` JSON convention).
 * `creator` is `Identity` serialized as `["0x<hex>"]` — we don't use it (we use
 * `creatorPrincipalText` instead). `createdAt` is `Timestamp` serialized as
 * `[<micros_since_epoch>]`.
 */
@Serializable
data class SpacetimePostDetails(
    val id: String,
    val description: String,
    val hashtags: List<String> = emptyList(),
    @SerialName("videoUid") val videoUid: String,
    val creator: JsonElement? = null,
    @SerialName("creatorPrincipalText") val creatorPrincipalText: String,
    @SerialName("createdAt") val createdAt: List<Long> = emptyList(),
    @SerialName("totalViewCount") val totalViewCount: ULong = 0u,
    @SerialName("likeCount") val likeCount: ULong = 0u,
    @SerialName("likedByMe") val likedByMe: Boolean = false,
    val status: SpacetimePostStatus = SpacetimePostStatus.Uploaded,
)

/**
 * SpacetimeDB `PostStatus` enum — unit variants serialized as plain strings.
 */
@Serializable
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
 * Wraps the `Option<PostDetailsForFrontend>` REST response.
 * SpacetimeDB encodes `Option` as a 2-element array: `[variant_index, payload]`.
 * - `Some(post)` → `[0, {post JSON}]`
 * - `None` → `[1, []]`
 *
 * The outer array wraps this: `[[variant_index, payload]]`.
 * We use `JsonElement` for the polymorphic payload and parse it after
 * inspecting the variant index.
 */
@Serializable
data class SpacetimeOptionResponse(
    val variantIndex: Int,
    val payload: JsonElement,
)
