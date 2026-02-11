package com.yral.shared.features.chat.ui.conversation

import com.yral.shared.features.chat.viewmodel.ConversationMessageItem

private const val LOCAL_KEY_PREFIX = "local-"
private const val REMOTE_KEY_PREFIX = "remote-"
private const val HISTORY_LOCAL_KEY_PREFIX = "history-local-"
private const val HISTORY_REMOTE_KEY_PREFIX = "history-remote-"
private const val HISTORY_PLACEHOLDER_KEY_PREFIX = "history-placeholder-"

internal fun overlayItemKey(item: ConversationMessageItem): String =
    when (item) {
        is ConversationMessageItem.Local -> "$LOCAL_KEY_PREFIX${item.message.localId}"
        is ConversationMessageItem.Remote -> "$REMOTE_KEY_PREFIX${item.message.id}"
    }

internal fun historyItemKey(
    item: ConversationMessageItem?,
    index: Int,
): String =
    when (item) {
        is ConversationMessageItem.Local -> "$HISTORY_LOCAL_KEY_PREFIX${item.message.localId}"
        is ConversationMessageItem.Remote -> "$HISTORY_REMOTE_KEY_PREFIX${item.message.id}"
        null -> "$HISTORY_PLACEHOLDER_KEY_PREFIX$index"
    }

internal fun ConversationMessageItem.messageId(): String =
    when (this) {
        is ConversationMessageItem.Local -> message.localId
        is ConversationMessageItem.Remote -> message.id
    }

internal fun overlayMessageIdSet(overlayItems: List<ConversationMessageItem>): Set<String> =
    overlayItems
        .map { it.messageId() }
        .toSet()

internal fun isDuplicateOfOverlay(
    item: ConversationMessageItem,
    overlayIds: Set<String>,
): Boolean = item.messageId() in overlayIds
