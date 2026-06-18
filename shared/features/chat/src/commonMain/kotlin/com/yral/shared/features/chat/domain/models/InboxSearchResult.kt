package com.yral.shared.features.chat.domain.models

/**
 * Single result row from the inbox conversation-search endpoint.
 * Carries enough state for the row to render (avatar + name + subtitle
 * + last-talked-to timestamp) and the conversation id needed to open
 * the chat directly on tap.
 */
data class InboxSearchResult(
    val conversationId: String,
    val influencerId: String,
    val name: String,
    val displayName: String,
    val avatarUrl: String,
    val category: String?,
    val subtitle: String?,
    val lastMessageAt: String?,
)
