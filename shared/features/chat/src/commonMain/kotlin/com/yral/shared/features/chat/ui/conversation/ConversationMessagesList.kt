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
import com.yral.shared.features.chat.domain.models.AssistantErrorPresentation
import com.yral.shared.features.chat.domain.models.ConversationMessageRole
import com.yral.shared.features.chat.domain.models.isFromCurrentUser
import com.yral.shared.features.chat.viewmodel.ConversationMessageItem
import com.yral.shared.features.chat.viewmodel.LocalMessageStatus

@Composable
internal fun MessagesList(
    modifier: Modifier,
    listState: LazyListState,
    overlayItems: List<ConversationMessageItem>,
    historyPagingItems: LazyPagingItems<ConversationMessageItem>,
    isBotAccount: Boolean = false,
    isHumanChat: Boolean = false,
    currentUserPrincipalId: String? = null,
    renderSystemBanners: Boolean = false,
    streamMarkdownLockedRemoteIds: Map<String, Boolean> = emptyMap(),
    assistantError: AssistantErrorPresentation? = null,
    onAssistantErrorRetry: () -> Unit = {},
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
        // Phase 6: the error bubble belongs in the assistant slot just below the
        // user's most recent message. With `reverseLayout = true`, items at the
        // start of this lambda render at the visual bottom — placing the error
        // item BEFORE the overlay items gives it the bottom-most slot so it sits
        // directly beneath the streaming Local that was just dropped on Failed.
        if (assistantError != null) {
            item(key = "assistant-error-${assistantError.error.rawCode}") {
                AssistantErrorBubble(
                    error = assistantError.error,
                    onRetry = if (assistantError.retryDraft != null) onAssistantErrorRetry else null,
                    modifier = Modifier.padding(vertical = MESSAGE_VERTICAL_PADDING_DP),
                )
            }
        }

        items(
            items = overlayItems,
        ) { item ->
            if (item.isSystemMessage() && !renderSystemBanners) return@items
            MessageRow(
                item = item,
                isBotAccount = isBotAccount,
                streamMarkdownLockedRemoteIds = streamMarkdownLockedRemoteIds,
                isHumanChat = isHumanChat,
                currentUserPrincipalId = currentUserPrincipalId,
                onImageClick = onImageClick,
                onRetry = onRetry,
            )
        }

        items(
            count = historyPagingItems.itemCount,
        ) { idx ->
            val item = historyPagingItems[idx] ?: return@items
            if (isDuplicateOfOverlay(item, overlayMessageIds)) return@items
            if (item.isSystemMessage() && !renderSystemBanners) return@items
            MessageRow(
                item = item,
                isBotAccount = isBotAccount,
                streamMarkdownLockedRemoteIds = streamMarkdownLockedRemoteIds,
                isHumanChat = isHumanChat,
                currentUserPrincipalId = currentUserPrincipalId,
                onImageClick = onImageClick,
                onRetry = onRetry,
            )
        }
    }
}

private fun ConversationMessageItem.isSystemMessage(): Boolean =
    when (this) {
        is ConversationMessageItem.Remote -> message.role == ConversationMessageRole.SYSTEM
        is ConversationMessageItem.Local -> message.role == ConversationMessageRole.SYSTEM
    }

// Renders the same slot for Remote vs Local items + the role-flip branch
// for bot accounts + the system-banner short-circuit. Unifying the
// Remote/Local render-param extraction in this function is precisely what
// eliminates the Local→Remote one-frame flicker at SSE `done` (see the
// inline comment block).
@Composable
@Suppress("CyclomaticComplexMethod")
private fun MessageRow(
    item: ConversationMessageItem,
    isBotAccount: Boolean,
    streamMarkdownLockedRemoteIds: Map<String, Boolean>,
    isHumanChat: Boolean,
    currentUserPrincipalId: String?,
    onImageClick: (imageUrl: String) -> Unit,
    onRetry: (localId: String) -> Unit,
) {
    val screenWidth = LocalWindowInfo.current.containerSize.width
    val maxWidth = with(LocalDensity.current) { (screenWidth * MESSAGE_MAX_WIDTH_RATIO).toDp() }
    val role =
        when (item) {
            is ConversationMessageItem.Remote -> item.message.role
            is ConversationMessageItem.Local -> item.message.role
        }
    if (role == ConversationMessageRole.SYSTEM && item is ConversationMessageItem.Remote) {
        SystemBannerMessage(
            text = item.message.content.orEmpty(),
            modifier = Modifier.fillMaxWidth().padding(vertical = MESSAGE_VERTICAL_PADDING_DP),
        )
        return
    }
    val roleIsUser = role == ConversationMessageRole.USER
    // Bubble-side discriminator:
    //   - Bot account (creator takeover): roles flipped — USER msgs come from
    //     the AI (left), ASSISTANT msgs come from the human creator (right).
    //   - H2H remote message: both peers send role='user' so role collapses
    //     as a discriminator. Use sender_id == viewer_principal instead.
    //   - H2H local optimistic add: always the viewer's own send → use
    //     roleIsUser (which is true for role='user' as expected).
    //   - AI: role-based — works because USER vs ASSISTANT are distinct.
    val isUser =
        when {
            isBotAccount -> !roleIsUser
            isHumanChat && item is ConversationMessageItem.Remote && currentUserPrincipalId != null ->
                item.message.isFromCurrentUser(currentUserPrincipalId)
            else -> roleIsUser
        }

    // Extract render params from either Remote or Local OUTSIDE the Box so the
    // MessageContent call below has a single slot-table entry. The Local→Remote
    // transition on SSE `done` (streaming placeholder swapped for the server
    // assistant message) lands at the same screen position; without a unified
    // slot, Compose tears down the Local subtree and creates a fresh Remote one
    // every time, producing a one-frame flicker.
    //
    // Cursor note: `renderContent` here is cursor-free (just the streamingBuffer
    // or the message content). RegularBubble renders the cursor as a sibling
    // composable, so the Markdown/Text renderer never sees the moving cursor.
    val renderContent: String?
    val renderMediaUrls: List<String>
    val renderIsFailed: Boolean
    val renderIsWaiting: Boolean
    val renderIsStreaming: Boolean
    val renderMarkdownLockedOverride: Boolean?
    val renderOnRetry: (() -> Unit)?
    when (item) {
        is ConversationMessageItem.Remote -> {
            renderContent = item.message.content
            renderMediaUrls = item.message.mediaUrls
            renderIsFailed = false
            renderIsWaiting = false
            renderIsStreaming = false
            // Phase 5b: look up the persisted lock for this server message id. Present
            // only for messages that arrived via streaming this VM session; absent for
            // history-paged messages, which fall back to the default Markdown decision.
            renderMarkdownLockedOverride = streamMarkdownLockedRemoteIds[item.message.id]
            renderOnRetry = null
        }

        is ConversationMessageItem.Local -> {
            val streamingBuffer = item.message.streamingBuffer
            renderContent =
                when {
                    streamingBuffer != null -> streamingBuffer
                    item.message.isPlaceholder -> "…"
                    else -> item.message.content
                }
            renderMediaUrls = item.message.mediaUrls
            renderIsFailed = item.message.status == LocalMessageStatus.FAILED
            renderIsWaiting = item.isWaitingAssistant()
            renderIsStreaming = streamingBuffer != null
            // Phase 5b: streaming Local carries its own per-stream path lock.
            renderMarkdownLockedOverride = item.message.useMarkdownLocked
            renderOnRetry =
                if (item.message.status == LocalMessageStatus.FAILED && !item.message.isPlaceholder) {
                    { onRetry(item.message.localId) }
                } else {
                    null
                }
        }
    }

    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = MESSAGE_VERTICAL_PADDING_DP),
        contentAlignment = if (isUser) Alignment.TopEnd else Alignment.TopStart,
    ) {
        MessageContent(
            isUser = isUser,
            content = renderContent,
            mediaUrls = renderMediaUrls,
            maxWidth = maxWidth,
            onImageClick = onImageClick,
            isFailed = renderIsFailed,
            isWaiting = renderIsWaiting,
            isStreaming = renderIsStreaming,
            markdownLockedOverride = renderMarkdownLockedOverride,
            onRetry = renderOnRetry,
        )
    }
}
