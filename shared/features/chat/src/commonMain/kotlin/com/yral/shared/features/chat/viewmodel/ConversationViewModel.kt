package com.yral.shared.features.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.shared.features.chat.domain.ChatRepository
import com.yral.shared.features.chat.domain.ConversationMessagesPagingSource
import com.yral.shared.features.chat.domain.models.ChatMessage
import com.yral.shared.features.chat.domain.models.ChatMessageType
import com.yral.shared.features.chat.domain.models.ConversationMessageRole
import com.yral.shared.features.chat.domain.models.SendMessageDraft
import com.yral.shared.features.chat.domain.usecases.SendMessageUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class ConversationViewModel(
    private val chatRepository: ChatRepository,
    private val sendMessageUseCase: SendMessageUseCase,
) : ViewModel() {
    /**
     * Ordering note for UI:
     * - Network paging uses `order=desc` (latest-first) so the initial page contains the newest messages.
     * - To render chats as **oldest at top / newest at bottom**, the UI should use
     *   `LazyColumn(reverseLayout = true)` and render **overlay first**, then **history**.
     *
     * With `reverseLayout = true`, the first items (newest) are laid out at the bottom, and paging
     * will naturally load older messages as you scroll upward.
     */
    private val conversationId = MutableStateFlow<String?>(null)
    private val historyVersion = MutableStateFlow(0)

    private val _overlay = MutableStateFlow(OverlayState())
    val overlay: StateFlow<List<ConversationMessageItem>> =
        _overlay
            .asStateFlow()
            .combine(conversationId) { overlayState, _ ->
                // newest-first across BOTH pending and sent (single ordering)
                buildList {
                    overlayState.pending.forEach { pending ->
                        add(pending.createdAtMs to ConversationMessageItem.Local(pending))
                    }
                    overlayState.sent.forEach { sent ->
                        add(sent.insertedAtMs to ConversationMessageItem.Remote(sent.message))
                    }
                }.sortedWith(compareByDescending { it.first }).map { it.second }
            }.distinctUntilChanged()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5.seconds.inWholeMilliseconds),
                emptyList(),
            )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val pagedHistory: Flow<PagingData<ChatMessage>> =
        combine(conversationId, historyVersion) { convId, _ -> convId }
            .flatMapLatest { convId ->
                if (convId.isNullOrBlank()) {
                    kotlinx.coroutines.flow.emptyFlow()
                } else {
                    Pager(
                        config =
                            PagingConfig(
                                pageSize = PAGE_SIZE,
                                initialLoadSize = PAGE_SIZE,
                                prefetchDistance = PREFETCH_DISTANCE,
                                enablePlaceholders = false,
                            ),
                        pagingSourceFactory = {
                            ConversationMessagesPagingSource(
                                conversationId = convId,
                                chatRepository = chatRepository,
                            )
                        },
                    ).flow
                }
            }.cachedIn(viewModelScope)

    val history: Flow<PagingData<ConversationMessageItem>> =
        pagedHistory
            .combine(_overlay) { pagingData, overlayState ->
                val sentIds = overlayState.sent.mapTo(HashSet()) { it.message.id }
                pagingData
                    .filter { msg -> msg.id !in sentIds }
                    .map { msg -> ConversationMessageItem.Remote(msg) as ConversationMessageItem }
            }.distinctUntilChanged()

    @OptIn(ExperimentalTime::class)
    fun setConversationId(
        id: String,
        recentMessages: List<ChatMessage> = emptyList(),
    ) {
        conversationId.value = id
        val sentMessages =
            recentMessages.mapNotNull { message ->
                runCatching {
                    Instant.parse(message.createdAt).toEpochMilliseconds()
                }.getOrNull()?.let { timestampMs ->
                    SentMessage(insertedAtMs = timestampMs, message = message)
                }
            }
        _overlay.value = OverlayState(sent = sentMessages)
        historyVersion.update { it + 1 } // Refresh paging source
        // Schedule clearing of recent messages from overlay after TTL
        if (sentMessages.isNotEmpty()) {
            scheduleSentOverlayClear()
        }
    }

    @Suppress("LongMethod")
    @OptIn(ExperimentalTime::class)
    fun sendMessage(draft: SendMessageDraft) {
        val convId = conversationId.value ?: return

        val now = Clock.System.now().toEpochMilliseconds()
        val localUserId = "local-user-$now"
        val localAssistantId = "local-assistant-$now"

        val userLocal =
            LocalMessage(
                localId = localUserId,
                role = ConversationMessageRole.USER,
                content = draft.content,
                messageType = draft.messageType,
                createdAtMs = now,
                status = LocalMessageStatus.SENDING,
                isPlaceholder = false,
                draftForRetry = draft,
            )
        val assistantPlaceholder =
            LocalMessage(
                localId = localAssistantId,
                role = ConversationMessageRole.ASSISTANT,
                content = null,
                messageType = ChatMessageType.TEXT,
                createdAtMs = now + 1,
                status = LocalMessageStatus.WAITING,
                isPlaceholder = true,
                draftForRetry = null,
            )

        _overlay.update {
            it.copy(pending = it.pending + userLocal + assistantPlaceholder)
        }

        viewModelScope.launch {
            sendMessageUseCase(
                SendMessageUseCase.Params(
                    conversationId = convId,
                    draft = draft,
                ),
            ).onSuccess { result ->
                _overlay.update { state ->
                    val remainingPending =
                        state.pending.filterNot { it.localId == localUserId || it.localId == localAssistantId }
                    val sentAt = Clock.System.now().toEpochMilliseconds()
                    val sentMessages =
                        buildList {
                            add(SentMessage(insertedAtMs = sentAt, message = result.userMessage))
                            // Ensure assistant message is newer than user message for correct ordering.
                            result.assistantMessage?.let { add(SentMessage(insertedAtMs = sentAt + 1, message = it)) }
                        }
                    state.copy(
                        pending = remainingPending,
                        sent = state.sent + sentMessages,
                    )
                }
                historyVersion.update { it + 1 }
                scheduleSentOverlayClear()
            }.onFailure { error ->
                _overlay.update { state ->
                    val updatedPending =
                        state.pending.mapNotNull { msg ->
                            when (msg.localId) {
                                localAssistantId -> null // remove placeholder
                                localUserId ->
                                    msg.copy(
                                        status = LocalMessageStatus.FAILED,
                                        errorMessage = error.message,
                                    )
                                else -> msg
                            }
                        }
                    state.copy(pending = updatedPending)
                }
            }
        }
    }

    @Suppress("LongMethod", "ReturnCount")
    @OptIn(ExperimentalTime::class)
    fun retry(localUserMessageId: String) {
        val convId = conversationId.value ?: return
        val pending = _overlay.value.pending.firstOrNull { it.localId == localUserMessageId } ?: return
        val draft = pending.draftForRetry ?: return

        val now = Clock.System.now().toEpochMilliseconds()
        val localAssistantId = "local-assistant-$now"
        val assistantPlaceholder =
            LocalMessage(
                localId = localAssistantId,
                role = ConversationMessageRole.ASSISTANT,
                content = null,
                messageType = ChatMessageType.TEXT,
                createdAtMs = now + 1,
                status = LocalMessageStatus.WAITING,
                isPlaceholder = true,
                draftForRetry = null,
            )

        _overlay.update { state ->
            state.copy(
                pending =
                    state.pending
                        .map { msg ->
                            if (msg.localId == localUserMessageId) {
                                msg.copy(status = LocalMessageStatus.SENDING, errorMessage = null)
                            } else {
                                msg
                            }
                        } + assistantPlaceholder,
            )
        }

        viewModelScope.launch {
            sendMessageUseCase(
                SendMessageUseCase.Params(
                    conversationId = convId,
                    draft = draft,
                ),
            ).onSuccess { result ->
                _overlay.update { state ->
                    val remainingPending =
                        state.pending.filterNot { it.localId == localUserMessageId || it.localId == localAssistantId }
                    val sentAt = Clock.System.now().toEpochMilliseconds()
                    val sentMessages =
                        buildList {
                            add(SentMessage(insertedAtMs = sentAt, message = result.userMessage))
                            result.assistantMessage?.let { add(SentMessage(insertedAtMs = sentAt + 1, message = it)) }
                        }
                    state.copy(
                        pending = remainingPending,
                        sent = state.sent + sentMessages,
                    )
                }
                historyVersion.update { it + 1 }
                scheduleSentOverlayClear()
            }.onFailure { error ->
                _overlay.update { state ->
                    val updatedPending =
                        state.pending.mapNotNull { msg ->
                            when (msg.localId) {
                                localAssistantId -> null
                                localUserMessageId ->
                                    msg.copy(
                                        status = LocalMessageStatus.FAILED,
                                        errorMessage = error.message,
                                    )
                                else -> msg
                            }
                        }
                    state.copy(pending = updatedPending)
                }
            }
        }
    }

    private data class OverlayState(
        val pending: List<LocalMessage> = emptyList(),
        val sent: List<SentMessage> = emptyList(),
    )

    private data class SentMessage(
        val insertedAtMs: Long,
        val message: ChatMessage,
    )

    private fun scheduleSentOverlayClear() {
        // Clear transient sent overlay after a short delay so history can show messages.
        viewModelScope.launch {
            delay(SENT_OVERLAY_TTL_MS)
            _overlay.update { it.copy(sent = emptyList()) }
        }
    }

    private companion object {
        private const val PAGE_SIZE = 50
        private const val PREFETCH_DISTANCE = 10
        private const val SENT_OVERLAY_TTL_MS = 1_000L
    }
}

sealed class ConversationMessageItem {
    data class Remote(
        val message: ChatMessage,
    ) : ConversationMessageItem()

    data class Local(
        val message: LocalMessage,
    ) : ConversationMessageItem()
}

enum class LocalMessageStatus {
    SENDING,
    WAITING,
    FAILED,
}

data class LocalMessage(
    val localId: String,
    val role: ConversationMessageRole,
    val content: String?,
    val messageType: ChatMessageType,
    val createdAtMs: Long,
    val status: LocalMessageStatus,
    val isPlaceholder: Boolean,
    val errorMessage: String? = null,
    val draftForRetry: SendMessageDraft?,
)
