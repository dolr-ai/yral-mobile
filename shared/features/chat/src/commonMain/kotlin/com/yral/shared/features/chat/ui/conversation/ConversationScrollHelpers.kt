package com.yral.shared.features.chat.ui.conversation

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.yral.shared.features.chat.domain.models.ConversationMessageRole
import com.yral.shared.features.chat.viewmodel.ConversationMessageItem
import com.yral.shared.features.chat.viewmodel.LocalMessageStatus

internal fun ConversationMessageItem.isAssistant() =
    when (this) {
        is ConversationMessageItem.Remote -> message.role == ConversationMessageRole.ASSISTANT
        is ConversationMessageItem.Local -> message.role == ConversationMessageRole.ASSISTANT
    }

internal fun ConversationMessageItem.isWaitingAssistant() =
    this is ConversationMessageItem.Local &&
        message.role == ConversationMessageRole.ASSISTANT &&
        message.status == LocalMessageStatus.WAITING

internal fun ConversationMessageItem.isUser() =
    when (this) {
        is ConversationMessageItem.Remote -> message.role == ConversationMessageRole.USER
        is ConversationMessageItem.Local -> message.role == ConversationMessageRole.USER
    }

internal fun ConversationMessageItem.isPendingUser() =
    this is ConversationMessageItem.Local &&
        message.role == ConversationMessageRole.USER &&
        message.status == LocalMessageStatus.SENDING

internal fun ConversationMessageItem.isFailedUser() =
    this is ConversationMessageItem.Local &&
        message.role == ConversationMessageRole.USER &&
        message.status == LocalMessageStatus.FAILED

@Suppress("MaxLineLength")
internal fun findLatestFailedUserIndex(overlayItems: List<ConversationMessageItem>): IndexedValue<ConversationMessageItem?> {
    overlayItems.forEachIndexed { idx, item ->
        if (item.isFailedUser()) return IndexedValue(idx, item)
    }
    return IndexedValue(-1, null)
}

@Suppress("ReturnCount")
internal fun findLatestAssistantIndex(
    overlayItems: List<ConversationMessageItem>,
    historyPagingItems: LazyPagingItems<ConversationMessageItem>,
): IndexedValue<ConversationMessageItem?> {
    overlayItems.forEachIndexed { idx, item ->
        if (item.isAssistant()) return IndexedValue(idx, item)
    }
    val historySnapshot = historyPagingItems.itemSnapshotList.items
    historySnapshot.forEachIndexed { idx, item ->
        if (item.isAssistant()) {
            return IndexedValue(overlayItems.size + idx, item)
        }
    }
    return IndexedValue(-1, null)
}

@Composable
internal fun AutoScrollToAssistantMessage(
    readyForAutoScroll: Boolean,
    latestAssistantMessage: ConversationMessageItem?,
    targetIndex: Int,
    listState: LazyListState,
    screenWidth: Int,
    density: Density,
    overlayItems: List<ConversationMessageItem>,
) {
    val failedUserState = findLatestFailedUserIndex(overlayItems)
    val hasFailedUser = failedUserState.index >= 0
    val isWaiting = latestAssistantMessage?.isWaitingAssistant() == true

    val scrollTarget by derivedStateOf {
        if (latestAssistantMessage == null || targetIndex < 0) {
            null
        } else {
            val pendingUserMessagesHeight =
                calculatePendingUserMessagesHeight(
                    overlayItems = overlayItems,
                    beforeIndex = targetIndex,
                    screenWidth = screenWidth,
                    density = density,
                )
            ScrollTarget(
                index = targetIndex,
                message = latestAssistantMessage,
                screenWidth = screenWidth,
                density = density,
                additionalOffset = pendingUserMessagesHeight,
                isWaiting = isWaiting,
            )
        }
    }

    LaunchedEffect(failedUserState.index, isWaiting) {
        if (hasFailedUser && !isWaiting) {
            runCatching {
                listState.animateScrollToItem(
                    index = failedUserState.index,
                    scrollOffset = 0,
                )
            }
        }
    }

    LaunchedEffect(scrollTarget, readyForAutoScroll) {
        val target = scrollTarget ?: return@LaunchedEffect
        if (!readyForAutoScroll && !target.isWaiting) return@LaunchedEffect
        runCatching {
            listState.animateScrollToItem(
                index = target.index,
                scrollOffset = target.scrollOffset,
            )
        }
    }
}

private fun calculatePendingUserMessagesHeight(
    overlayItems: List<ConversationMessageItem>,
    beforeIndex: Int,
    screenWidth: Int,
    density: Density,
): Int {
    var totalHeight = 0
    val messageSpacing = with(density) { MESSAGE_VERTICAL_PADDING_DP.toPx().toInt() * 2 }

    for (i in 0 until beforeIndex.coerceAtMost(overlayItems.size)) {
        val item = overlayItems[i]
        if (item.isPendingUser() && item is ConversationMessageItem.Local) {
            totalHeight +=
                estimateMessageHeight(
                    content = item.message.content.orEmpty(),
                    mediaUrls = item.message.mediaUrls,
                    screenWidth = screenWidth,
                    density = density,
                )
            totalHeight += messageSpacing
        }
    }
    return totalHeight
}

private data class ScrollTarget(
    val index: Int,
    val message: ConversationMessageItem,
    val screenWidth: Int,
    val density: Density,
    val additionalOffset: Int = 0,
    val isWaiting: Boolean = false,
) {
    val scrollOffset: Int
        get() =
            if (isWaiting) {
                additionalOffset
            } else {
                val (content, mediaUrls) =
                    when (message) {
                        is ConversationMessageItem.Remote ->
                            message.message.content.orEmpty() to message.message.mediaUrls
                        is ConversationMessageItem.Local ->
                            message.message.content.orEmpty() to message.message.mediaUrls
                    }
                val messageHeight = estimateMessageHeight(content, mediaUrls, screenWidth, density)
                ((messageHeight * SCROLL_OFFSET_RATIO) + additionalOffset).toInt()
            }
}

internal fun estimateMessageHeight(
    content: String,
    mediaUrls: List<String>,
    screenWidth: Int,
    density: Density,
): Int {
    val lineHeight = with(density) { 20.dp.toPx() }
    val charWidth = with(density) { 8.dp.toPx() }
    val horizontalPadding = with(density) { 16.dp.toPx() }
    val verticalPadding = with(density) { 16.dp.toPx() }

    val availableWidth = screenWidth * MESSAGE_MAX_WIDTH_RATIO - horizontalPadding
    val charsPerLine = (availableWidth / charWidth).toInt().coerceAtLeast(1)
    val estimatedLines = (content.length / charsPerLine.toFloat()).toInt() + 1

    val mediaHeight = with(density) { CHAT_MEDIA_IMAGE_SIZE.toPx() + 8.dp.toPx() }

    val estimatedHeight = ((estimatedLines * lineHeight) + (mediaUrls.size * mediaHeight) + verticalPadding).toInt()
    return estimatedHeight.coerceAtLeast(with(density) { 36.dp.toPx() }.toInt())
}
