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
    isBotAccount: Boolean = false,
    onImageClick: (imageUrl: String) -> Unit,
    onRetry: (localId: String) -> Unit,
) {
    val overlayMessageIds = overlayMessageIdSet(overlayItems)

    LazyColumn(
        state = listState,
        modifier = modifier,
        // Newest messages at bottom
        reverseLayout = true,
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        items(
            items = overlayItems,
        ) { item ->
            MessageRow(
                item = item,
                isBotAccount = isBotAccount,
                onImageClick = onImageClick,
                onRetry = onRetry,
            )
        }

        items(
            count = historyPagingItems.itemCount,
        ) { idx ->
            val item = historyPagingItems[idx] ?: return@items
            if (isDuplicateOfOverlay(item, overlayMessageIds)) return@items
            MessageRow(
                item = item,
                isBotAccount = isBotAccount,
                onImageClick = onImageClick,
                onRetry = onRetry,
            )
        }
    }
}

@Composable
private fun MessageRow(
    item: ConversationMessageItem,
    isBotAccount: Boolean,
    onImageClick: (imageUrl: String) -> Unit,
    onRetry: (localId: String) -> Unit,
) {
    val screenWidth = LocalWindowInfo.current.containerSize.width
    val maxWidth = with(LocalDensity.current) { (screenWidth * MESSAGE_MAX_WIDTH_RATIO).toDp() }
    val roleIsUser =
        when (item) {
            is ConversationMessageItem.Remote -> item.message.role == ConversationMessageRole.USER
            is ConversationMessageItem.Local -> item.message.role == ConversationMessageRole.USER
        }
    // For bot accounts, roles are flipped: USER messages come from the AI (shown on left),
    // ASSISTANT messages come from the human customer (shown on right).
    val isUser = if (isBotAccount) !roleIsUser else roleIsUser

    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = MESSAGE_VERTICAL_PADDING_DP),
        contentAlignment = if (isUser) Alignment.TopEnd else Alignment.TopStart,
    ) {
        when (item) {
            is ConversationMessageItem.Remote -> {
                MessageContent(
                    isUser = isUser,
                    content = item.message.content,
                    mediaUrls = item.message.mediaUrls,
                    maxWidth = maxWidth,
                    onImageClick = onImageClick,
                )
            }

            is ConversationMessageItem.Local -> {
                MessageContent(
                    isUser = isUser,
                    content = if (item.message.isPlaceholder) "…" else item.message.content,
                    mediaUrls = item.message.mediaUrls,
                    maxWidth = maxWidth,
                    onImageClick = onImageClick,
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
