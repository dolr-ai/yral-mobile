package com.yral.shared.features.chat.domain.models

/**
 * Domain representation of a paginated influencers response.
 *
 * Note: [rawCount] represents the number of items returned by the API for this page (before applying
 * any domain filtering). This is used to compute pagination correctly even if some items are filtered
 * out (e.g. inactive influencers).
 */
data class InfluencersPageResult(
    val influencers: List<Influencer>,
    val total: Int,
    val limit: Int,
    val offset: Int,
    val nextOffset: Int?,
    val rawCount: Int,
)
