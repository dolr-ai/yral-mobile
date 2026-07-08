package com.yral.shared.features.chat.domain.models

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val role: ConversationMessageRole,
    val content: String?,
    val messageType: ChatMessageType,
    val mediaUrls: List<String>,
    val audioUrl: String?,
    val audioDurationSeconds: Int?,
    val tokenCount: Int?,
    val createdAt: String,
    // H2H: the principal_id of whichever user inserted this row. Null on
    // legacy rows from before the backend exposed sender_id (handled by
    // [isFromCurrentUser]'s null-fallback). For AI chats this carries
    // through but isn't load-bearing — role alone is authoritative there.
    val senderId: String? = null,
    // When true, image attachments are shown blurred behind a pay-to-unlock
    // overlay until the user purchases access to them.
    val isBlur: Boolean = false,
    // COLLAGE messages store only this reference (never URLs); the bubble
    // fetches the image set at render time with the current subscription state.
    val collageBotId: String? = null,
    val collageDate: String? = null,
)

/**
 * Side-of-conversation derivation. The H2H send path stores every message
 * with role="user" regardless of sender, so role alone can't tell the
 * mobile client which side authored a row — sender_id is what
 * `ChatConversationScreen` uses to align the bubble (right-side current
 * user vs left-side participant).
 *
 * The `senderId == null` fallback keeps AI chat and legacy H2H rows
 * (inserted before the backend started exposing sender_id) working by
 * deferring to the existing role check. New code can use this property
 * uniformly across conversation kinds.
 */
fun ChatMessage.isFromCurrentUser(currentUserPrincipal: String): Boolean =
    senderId == currentUserPrincipal ||
        (senderId == null && role == ConversationMessageRole.USER)
