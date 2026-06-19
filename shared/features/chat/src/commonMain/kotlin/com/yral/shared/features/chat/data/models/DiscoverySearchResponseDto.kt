package com.yral.shared.features.chat.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Discovery search response — `GET /api/v2/discovery/search?q=&limit=`.
 *
 * Backend is pg_trgm-backed across name + category + archetype +
 * description with a tie-break on message_count. Active bots only.
 * v1 of the feature always returns `kind = "influencer"`; the field is
 * kept on the contract so a future user-directory search can slot in
 * without breaking mobile.
 */
@Serializable
data class DiscoverySearchResponseDto(
    @SerialName("results")
    val results: List<DiscoverySearchResultDto> = emptyList(),
    @SerialName("count")
    val count: Int = 0,
)

@Serializable
data class DiscoverySearchResultDto(
    @SerialName("kind")
    val kind: String = "influencer",
    @SerialName("id")
    val id: String,
    @SerialName("name")
    val name: String,
    @SerialName("display_name")
    val displayName: String,
    @SerialName("avatar_url")
    val avatarUrl: String,
    @SerialName("description")
    val description: String? = null,
    @SerialName("category")
    val category: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    /**
     * Server-pre-formatted "archetype · category" string for direct
     * subtitle rendering. Optional in case the backend evolves to
     * client-side formatting.
     */
    @SerialName("subtitle")
    val subtitle: String? = null,
)
