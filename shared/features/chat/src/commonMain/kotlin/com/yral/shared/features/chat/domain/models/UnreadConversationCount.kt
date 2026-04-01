package com.yral.shared.features.chat.domain.models

private const val CHAT_TAB_UNREAD_BADGE_CAP = 99

internal fun Conversation.countsTowardUnreadConversationBadge(): Boolean = unreadCount > 0 && conversationUser == null

internal fun Iterable<Conversation>.totalUnreadConversationBadgeCount(): Int =
    sumOf { conversation ->
        if (conversation.countsTowardUnreadConversationBadge()) {
            conversation.unreadCount
        } else {
            0
        }
    }

fun formatChatUnreadBadgeCount(count: Int): String? =
    when {
        count <= 0 -> null
        count > CHAT_TAB_UNREAD_BADGE_CAP -> "$CHAT_TAB_UNREAD_BADGE_CAP+"
        else -> count.toString()
    }
