package com.yral.shared.features.chat.ui.conversation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.yral.shared.features.chat.domain.models.AssistantErrorPresentation
import com.yral.shared.features.chat.domain.models.ChatMessageType
import com.yral.shared.features.chat.domain.models.ConversationMessageRole
import com.yral.shared.features.chat.domain.models.isFromCurrentUser
import com.yral.shared.features.chat.viewmodel.CollageUiState
import com.yral.shared.features.chat.viewmodel.ConversationMessageItem
import com.yral.shared.features.chat.viewmodel.LocalMessageStatus
import kotlinx.datetime.LocalDate
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

// The list's feature surface (paging + overlay + streaming locks + assistant
// errors + blur/unlock + collage) is inherently wide; related inputs are
// already bundled (CollageListConfig, AssistantErrorPresentation).
@Suppress("LongParameterList")
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
    collageConfig: CollageListConfig = CollageListConfig(),
    onImageClick: (imageUrl: String) -> Unit,
    onUnlockImage: (messageId: String) -> Unit,
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
                collageConfig = collageConfig,
                onImageClick = onImageClick,
                onUnlockImage = onUnlockImage,
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
                collageConfig = collageConfig,
                onImageClick = onImageClick,
                onUnlockImage = onUnlockImage,
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
    collageConfig: CollageListConfig,
    onImageClick: (imageUrl: String) -> Unit,
    onUnlockImage: (messageId: String) -> Unit,
    onRetry: (localId: String) -> Unit,
) {
    val screenWidth = LocalWindowInfo.current.containerSize.width
    val maxWidth = with(LocalDensity.current) { (screenWidth * MESSAGE_MAX_WIDTH_RATIO).toDp() }
    val role = item.role()
    if (role == ConversationMessageRole.SYSTEM && item is ConversationMessageItem.Remote) {
        SystemBannerMessage(
            text = item.message.content.orEmpty(),
            modifier = Modifier.fillMaxWidth().padding(vertical = MESSAGE_VERTICAL_PADDING_DP),
        )
        return
    }

    // Collage messages render their own bubble: the reference is resolved at
    // render time (see CollageBubble). Always left-aligned — the photos are
    // the influencer's, whatever role the send endpoint stored.
    val collageInfo = item.collageDisplayInfoOrNull(fallbackBotId = collageConfig.botId)
    if (collageInfo != null) {
        CollageMessageRow(
            info = collageInfo,
            config = collageConfig,
            maxWidth = maxWidth,
        )
        return
    }
    // Bubble-side discriminator:
    //   - Bot account (creator takeover): roles flipped — USER msgs come from
    //     the AI (left), ASSISTANT msgs come from the human creator (right).
    //   - H2H remote message: both peers send role='user' so role collapses
    //     as a discriminator. Use sender_id == viewer_principal instead.
    //   - H2H local optimistic add: always the viewer's own send → use
    //     roleIsUser (which is true for role='user' as expected).
    //   - AI: role-based — works because USER vs ASSISTANT are distinct.
    val isUser =
        item.isCurrentUserMessage(
            isBotAccount = isBotAccount,
            isHumanChat = isHumanChat,
            currentUserPrincipalId = currentUserPrincipalId,
        )

    // Extract render params from either Remote or Local OUTSIDE the Box so the
    // MessageContent call below has a single slot-table entry. The Local→Remote
    // transition on SSE `done` (streaming placeholder swapped for the server
    // assistant message) lands at the same screen position; without a unified
    // slot, Compose tears down the Local subtree and creates a fresh Remote one
    // every time, producing a one-frame flicker.
    val renderParams =
        item.toRenderParams(
            streamMarkdownLockedRemoteIds = streamMarkdownLockedRemoteIds,
            onUnlockImage = onUnlockImage,
            onRetry = onRetry,
        )

    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = MESSAGE_VERTICAL_PADDING_DP),
        contentAlignment = if (isUser) Alignment.TopEnd else Alignment.TopStart,
    ) {
        MessageContent(
            isUser = isUser,
            content = renderParams.content,
            mediaUrls = renderParams.mediaUrls,
            maxWidth = maxWidth,
            onImageClick = onImageClick,
            isFailed = renderParams.isFailed,
            isWaiting = renderParams.isWaiting,
            isStreaming = renderParams.isStreaming,
            markdownLockedOverride = renderParams.markdownLockedOverride,
            onRetry = renderParams.onRetry,
            isBlurred = renderParams.isBlurred,
            onUnlockClick = renderParams.onUnlockClick,
        )
    }
}

/** Collage inputs bundled so the list/row signatures stay within detekt's parameter budget. */
internal data class CollageListConfig(
    val states: Map<String, CollageUiState> = emptyMap(),
    // The conversation's influencer id — fallback bot for collage rows the
    // backend stored without their reference fields.
    val botId: String = "",
    val influencerDisplayName: String = "",
    val onLoad: (botId: String, date: String, collageId: String?) -> Unit = { _, _, _ -> },
    val onSubscribeClick: () -> Unit = {},
    // Full-screen preview with the whole collage as a swipeable gallery.
    val onImagePreview: (images: List<String>, imageUrl: String) -> Unit = { _, _ -> },
)

@Composable
private fun CollageMessageRow(
    info: CollageDisplayInfo,
    config: CollageListConfig,
    maxWidth: Dp,
) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = MESSAGE_VERTICAL_PADDING_DP),
        contentAlignment = Alignment.TopStart,
    ) {
        Box(modifier = Modifier.widthIn(max = maxWidth)) {
            if (info.isGenerating || info.botId == null || info.date == null) {
                CollageGeneratingBubble(influencerName = config.influencerDisplayName)
            } else {
                CollageBubble(
                    botId = info.botId,
                    date = info.date,
                    // Key format mirrors ConversationViewModel.collageKey.
                    state = config.states["${info.botId}|${info.date}"],
                    influencerName = config.influencerDisplayName,
                    onLoad = { config.onLoad(info.botId, info.date, info.collageId) },
                    onImageClick = config.onImagePreview,
                    onSubscribeClick = config.onSubscribeClick,
                    maxWidth = maxWidth,
                )
            }
        }
    }
}

private data class CollageDisplayInfo(
    val isGenerating: Boolean,
    val botId: String?,
    val date: String?,
    // Preferred fetch handle; null on legacy messages, which resolve by date.
    val collageId: String?,
)

/**
 * Non-null for COLLAGE messages that should render the collage bubble.
 *
 * Remote rows the backend stored WITHOUT their reference fields (known gap)
 * self-heal: the bot is the conversation's influencer ([fallbackBotId]) and
 * the collage date is the message's own UTC send date — GET /collage?date=
 * resolves both. Rows where even that fails fall back to the regular text
 * bubble (their content is the "Requested an image" fallback).
 */
private fun ConversationMessageItem.collageDisplayInfoOrNull(fallbackBotId: String?): CollageDisplayInfo? {
    val info =
        when (this) {
            is ConversationMessageItem.Remote ->
                message
                    .takeIf { it.messageType == ChatMessageType.COLLAGE }
                    ?.let {
                        CollageDisplayInfo(
                            isGenerating = false,
                            botId = it.collageBotId ?: fallbackBotId?.takeIf(String::isNotBlank),
                            date = it.collageDate ?: utcDateStringOrNull(it.createdAt),
                            collageId = it.collageId,
                        )
                    }

            is ConversationMessageItem.Local ->
                message
                    .takeIf { it.messageType == ChatMessageType.COLLAGE }
                    ?.let {
                        CollageDisplayInfo(
                            isGenerating = it.isPlaceholder,
                            botId = it.collageBotId,
                            date = it.collageDate,
                            collageId = it.collageId,
                        )
                    }
        } ?: return null
    val missingReference = !info.isGenerating && (info.botId == null || info.date == null)
    return if (missingReference) null else info
}

/** UTC calendar date ("YYYY-MM-DD") of an ISO timestamp, null if unparseable. */
@OptIn(ExperimentalTime::class)
private fun utcDateStringOrNull(createdAt: String): String? =
    runCatching {
        val normalized =
            if (createdAt.endsWith('Z') || createdAt.matches(Regex(".*[+-]\\d{2}:\\d{2}$"))) {
                createdAt
            } else {
                "${createdAt}Z"
            }
        val epochDays = Instant.parse(normalized).toEpochMilliseconds().floorDiv(MS_PER_UTC_DAY)
        LocalDate.fromEpochDays(epochDays.toInt()).toString()
    }.getOrNull()

private const val MS_PER_UTC_DAY = 86_400_000L

private data class MessageRenderParams(
    val content: String?,
    val mediaUrls: List<String>,
    val isFailed: Boolean,
    val isWaiting: Boolean,
    val isStreaming: Boolean,
    val markdownLockedOverride: Boolean?,
    val onRetry: (() -> Unit)?,
    val isBlurred: Boolean,
    val onUnlockClick: (() -> Unit)?,
)

// Cursor note: `content` here is cursor-free (just the streamingBuffer
// or the message content). RegularBubble renders the cursor as a sibling
// composable, so the Markdown/Text renderer never sees the moving cursor.
private fun ConversationMessageItem.toRenderParams(
    streamMarkdownLockedRemoteIds: Map<String, Boolean>,
    onUnlockImage: (messageId: String) -> Unit,
    onRetry: (localId: String) -> Unit,
): MessageRenderParams =
    when (this) {
        is ConversationMessageItem.Remote ->
            MessageRenderParams(
                content = message.content,
                mediaUrls = message.mediaUrls,
                isFailed = false,
                isWaiting = false,
                isStreaming = false,
                // Phase 5b: look up the persisted lock for this server message id. Present
                // only for messages that arrived via streaming this VM session; absent for
                // history-paged messages, which fall back to the default Markdown decision.
                markdownLockedOverride = streamMarkdownLockedRemoteIds[message.id],
                onRetry = null,
                isBlurred = message.isBlur,
                onUnlockClick = { onUnlockImage(message.id) },
            )

        is ConversationMessageItem.Local -> {
            val streamingBuffer = message.streamingBuffer
            MessageRenderParams(
                content =
                    when {
                        streamingBuffer != null -> streamingBuffer
                        message.isPlaceholder -> "…"
                        else -> message.content
                    },
                mediaUrls = message.mediaUrls,
                isFailed = message.status == LocalMessageStatus.FAILED,
                isWaiting = isWaitingAssistant(),
                isStreaming = streamingBuffer != null,
                // Phase 5b: streaming Local carries its own per-stream path lock.
                markdownLockedOverride = message.useMarkdownLocked,
                onRetry =
                    if (message.status == LocalMessageStatus.FAILED && !message.isPlaceholder) {
                        { onRetry(message.localId) }
                    } else {
                        null
                    },
                // Optimistic local sends are always the viewer's own images.
                isBlurred = false,
                onUnlockClick = null,
            )
        }
    }

private fun ConversationMessageItem.role(): ConversationMessageRole =
    when (this) {
        is ConversationMessageItem.Remote -> {
            message.role
        }

        is ConversationMessageItem.Local -> {
            message.role
        }
    }

private fun ConversationMessageItem.isCurrentUserMessage(
    isBotAccount: Boolean,
    isHumanChat: Boolean,
    currentUserPrincipalId: String?,
): Boolean {
    val roleIsUser =
        when (this) {
            is ConversationMessageItem.Remote -> {
                if (isHumanChat && currentUserPrincipalId != null) {
                    message.isFromCurrentUser(currentUserPrincipalId)
                } else {
                    message.role == ConversationMessageRole.USER
                }
            }

            is ConversationMessageItem.Local -> {
                message.role == ConversationMessageRole.USER
            }
        }
    return if (isBotAccount) !roleIsUser else roleIsUser
}
