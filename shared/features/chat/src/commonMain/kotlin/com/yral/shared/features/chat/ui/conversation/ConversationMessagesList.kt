package com.yral.shared.features.chat.ui.conversation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.yral.shared.features.chat.domain.models.ConversationMessageRole
import com.yral.shared.features.chat.viewmodel.ConversationMessageItem
import com.yral.shared.features.chat.viewmodel.LocalMessageStatus

@Composable
internal fun MessagesList(
    modifier: Modifier,
    listState: LazyListState,
    overlayItems: List<ConversationMessageItem>,
    historyPagingItems: LazyPagingItems<ConversationMessageItem>,
    onRetry: (localId: String) -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = modifier,
        reverseLayout = true, // newest at bottom
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        items(
            items = overlayItems,
            key = { item ->
                when (item) {
                    is ConversationMessageItem.Local -> "local-${item.message.localId}"
                    is ConversationMessageItem.Remote -> "remote-${item.message.id}"
                }
            },
        ) { item ->
            MessageRow(item = item, onRetry = onRetry)
        }

        items(count = historyPagingItems.itemCount) { idx ->
            val item = historyPagingItems[idx] ?: return@items
            MessageRow(item = item, onRetry = onRetry)
        }
    }
}

@Composable
private fun MessageRow(
    item: ConversationMessageItem,
    onRetry: (localId: String) -> Unit,
) {
    val screenWidth = LocalWindowInfo.current.containerSize.width
    val maxWidth = with(LocalDensity.current) { (screenWidth * MESSAGE_MAX_WIDTH_RATIO).toDp() }
    val isUser =
        when (item) {
            is ConversationMessageItem.Remote -> item.message.role == ConversationMessageRole.USER
            is ConversationMessageItem.Local -> item.message.role == ConversationMessageRole.USER
        }

    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = MESSAGE_VERTICAL_PADDING_DP),
        contentAlignment = if (isUser) Alignment.TopEnd else Alignment.TopStart,
    ) {
        when (item) {
            is ConversationMessageItem.Remote -> {
                MessageContent(
                    role = item.message.role,
                    content = item.message.content,
                    mediaUrls = item.message.mediaUrls,
                    maxWidth = maxWidth,
                )
            }

            is ConversationMessageItem.Local -> {
                MessageContent(
                    role = item.message.role,
                    content = if (item.message.isPlaceholder) "…" else item.message.content,
                    mediaUrls = item.message.mediaUrls,
                    maxWidth = maxWidth,
                    isFailed = item.message.status == LocalMessageStatus.FAILED,
                    isWaiting = item.isWaitingAssistant(),
                    onRetry =
                        if (item.message.status == LocalMessageStatus.FAILED && !item.message.isPlaceholder) {
                            { onRetry(item.message.localId) }
                        } else {
                            null
                        },
                )
            }
        }
    }
}
