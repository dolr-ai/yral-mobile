package com.yral.shared.features.chat.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Inbox search response — `GET /api/v2/chat/conversations/search?q=&limit=`.
 *
 * Mirrors the discovery-search envelope (`results` + `count`) with two
 * additions per row: `conversation_id` so a tap navigates straight into
 * the existing chat and `last_message_at` so the row can show when the
 * creator last talked to that bot. Defensive nullability on every field
 * — the inbox endpoint is being built in parallel; this lets the shape
 * evolve without a `MissingFieldException` on mobile.
 */
@Serializable
data class InboxSearchResponseDto(
    @SerialName("results")
    val results: List<InboxSearchResultDto> = emptyList(),
    @SerialName("count")
    val count: Int = 0,
)

@Serializable
data class InboxSearchResultDto(
    @SerialName("conversation_id")
    val conversationId: String,
    @SerialName("influencer_id")
    val influencerId: String,
    @SerialName("name")
    val name: String? = null,
    @SerialName("display_name")
    val displayName: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("category")
    val category: String? = null,
    /**
     * Server-pre-formatted "archetype · category" subtitle. Mirrors the
     * discovery search behaviour — mobile strips a leading "unknown · "
     * during the classifier backfill window.
     */
    @SerialName("subtitle")
    val subtitle: String? = null,
    @SerialName("last_message_at")
    val lastMessageAt: String? = null,
)
