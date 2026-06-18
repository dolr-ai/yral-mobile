package com.yral.shared.features.chat.domain.models

/**
 * Single search result from the Discovery v2 search endpoint.
 * Distinct from [Influencer] because search ships fewer fields than the
 * feed (no signals / status / counts), and we want a tighter contract
 * for the search-results UI.
 */
data class DiscoverySearchResult(
    val kind: SearchResultKind,
    val id: String,
    /**
     * Slug-style backend `name` — used as the conversation `username` so
     * the chat opens with the same metadata the feed cards open with.
     */
    val name: String,
    val displayName: String,
    val avatarUrl: String,
    /**
     * Bot category (e.g. "anime", "food"). Kept so search-tapped chats
     * carry the same analytics payload feed-tapped chats do.
     */
    val category: String?,
    /**
     * Server-pre-formatted "archetype · category" subtitle. Null when
     * neither archetype nor category was set on the bot; the UI hides
     * the subtitle line in that case.
     */
    val subtitle: String?,
)

enum class SearchResultKind {
    INFLUENCER,
    USER,
    UNKNOWN,
    ;

    companion object {
        fun fromString(value: String?): SearchResultKind =
            when (value?.lowercase()?.trim()) {
                "influencer" -> INFLUENCER
                "user" -> USER
                else -> UNKNOWN
            }
    }
}
