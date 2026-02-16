package com.yral.shared.features.chat.ui.conversation

import com.yral.shared.features.chat.viewmodel.ConversationMessageItem

private const val OVERLAY_KEY_PREFIX = "overlay-"
private const val HISTORY_KEY_PREFIX = "history-"

internal fun overlayItemKey(index: Int): String = "$OVERLAY_KEY_PREFIX$index"

internal fun historyItemKey(index: Int): String = "$HISTORY_KEY_PREFIX$index"

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
