package com.yral.shared.features.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.shared.features.chat.attachments.ChatAttachment
import com.yral.shared.features.chat.attachments.FilePathChatAttachment
import com.yral.shared.features.chat.domain.ChatRepository
import com.yral.shared.features.chat.domain.ConversationMessagesPagingSource
import com.yral.shared.features.chat.domain.models.ChatMessage
import com.yral.shared.features.chat.domain.models.ChatMessageType
import com.yral.shared.features.chat.domain.models.ConversationInfluencer
import com.yral.shared.features.chat.domain.models.ConversationMessageRole
import com.yral.shared.features.chat.domain.models.SendMessageDraft
import com.yral.shared.features.chat.domain.models.SendMessageResult
import com.yral.shared.features.chat.domain.usecases.CreateConversationUseCase
import com.yral.shared.features.chat.domain.usecases.DeleteConversationUseCase
import com.yral.shared.features.chat.domain.usecases.SendMessageUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
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
    private val createConversationUseCase: CreateConversationUseCase,
    private val deleteConversationUseCase: DeleteConversationUseCase,
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
    private val _viewState = MutableStateFlow(ConversationViewState())
    val viewState: StateFlow<ConversationViewState> = _viewState.asStateFlow()

    private val conversationId: String?
        get() = _viewState.value.conversationId
    private val loadedMessageIds = MutableStateFlow<Set<String>>(emptySet())
    private val initialOffset = MutableStateFlow<Int?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val pagedHistory: Flow<PagingData<ChatMessage>> =
        _viewState
            .map { it.conversationId }
            .distinctUntilChanged()
            .flatMapLatest { convId ->
                if (convId.isNullOrBlank()) {
                    emptyFlow()
                } else {
                    loadedMessageIds.value = emptySet()
                    // Capture and reset initialOffset (one-time use per conversation)
                    val initialOffset =
                        initialOffset.value
                            .takeIf { initialOffset.value != null }
                            .also { initialOffset.value = null }
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
                                initialOffset = initialOffset,
                            )
                        },
                    ).flow
                }
            }.cachedIn(viewModelScope)
    val history: Flow<PagingData<ConversationMessageItem>> =
        pagedHistory.map { pagingData ->
            pagingData.map { message ->
                val currentIds = loadedMessageIds.value
                if (message.id !in currentIds) {
                    loadedMessageIds.update { it + message.id }
                }
                ConversationMessageItem.Remote(message) as ConversationMessageItem
            }
        }

    private val _overlay = MutableStateFlow(OverlayState())
    val overlay: StateFlow<List<ConversationMessageItem>> =
        combine(_overlay, loadedMessageIds) { overlayState, loadedIds ->
            val filteredSent = overlayState.sent.filterNot { it.message.id in loadedIds }
            buildList {
                overlayState.pending.forEach { pending ->
                    add(pending.createdAtMs to ConversationMessageItem.Local(pending))
                }
                filteredSent.forEach { sent ->
                    add(sent.insertedAtMs to ConversationMessageItem.Remote(sent.message))
                }
            }.sortedWith(compareByDescending { it.first }).map { it.second }
        }.distinctUntilChanged()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5.seconds.inWholeMilliseconds),
                emptyList(),
            )

    init {
        createConversation(TEST_INFLUENCER_ID)
    }

    @OptIn(ExperimentalTime::class)
    private fun parseTimestampToEpochMs(timestamp: String): Long? =
        runCatching {
            val normalizedTimestamp =
                if (timestamp.endsWith('Z') || timestamp.matches(Regex(".*[+-]\\d{2}:\\d{2}$"))) {
                    timestamp
                } else {
                    "${timestamp}Z"
                }
            Instant.parse(normalizedTimestamp).toEpochMilliseconds()
        }.getOrNull()

    private fun setConversationId(
        id: String,
        influencer: ConversationInfluencer? = null,
        recentMessages: List<ChatMessage> = emptyList(),
    ) {
        val previousConversationId = _viewState.value.conversationId

        // Set initial offset BEFORE updating conversationId to ensure it's captured when Pager is created
        // This prevents duplicate API call for the first page
        if (recentMessages.isNotEmpty()) {
            initialOffset.value = recentMessages.size
            // Mark recentMessages as loaded so they're filtered from overlay once pagedHistory loads
            val recentMessageIds = recentMessages.mapTo(HashSet()) { it.id }
            loadedMessageIds.update { it + recentMessageIds }
        }

        _viewState.update { it.copy(conversationId = id, influencer = influencer ?: it.influencer) }

        if (previousConversationId != id) {
            _overlay.value = OverlayState()
        }

        val sentMessages =
            recentMessages.mapNotNull { message ->
                parseTimestampToEpochMs(message.createdAt)?.let { timestampMs ->
                    SentMessage(insertedAtMs = timestampMs, message = message)
                }
            }
        _overlay.update { it.copy(sent = it.sent + sentMessages) }
    }

    fun createConversation(influencerId: String) {
        _viewState.update { it.copy(isCreating = true, error = null) }
        viewModelScope.launch {
            createConversationUseCase(CreateConversationUseCase.Params(influencerId = influencerId))
                .onSuccess { conversation ->
                    setConversationId(
                        id = conversation.id,
                        influencer = conversation.influencer,
                        recentMessages = conversation.recentMessages,
                    )
                    _viewState.update { it.copy(isCreating = false) }
                }.onFailure { error ->
                    _viewState.update {
                        it.copy(
                            isCreating = false,
                            error = error.message ?: "Failed to create conversation",
                        )
                    }
                }
        }
    }

    fun deleteAndRecreateConversation(influencerId: String) {
        val currentConversationId = _viewState.value.conversationId ?: return
        _viewState.update { it.copy(isDeleting = true, error = null) }
        viewModelScope.launch {
            deleteConversationUseCase(DeleteConversationUseCase.Params(conversationId = currentConversationId))
                .onSuccess {
                    // After successful deletion, create a new conversation
                    createConversation(influencerId)
                    _viewState.update { it.copy(isDeleting = false) }
                }.onFailure { error ->
                    _viewState.update {
                        it.copy(
                            isDeleting = false,
                            isCreating = false,
                            error = error.message ?: "Failed to delete conversation",
                        )
                    }
                }
        }
    }

    private fun ChatAttachment.getFilePathOrNull(): String? = (this as? FilePathChatAttachment)?.filePath

    private fun extractMediaUrls(attachments: List<ChatAttachment>): List<String> =
        attachments
            .mapNotNull { it.getFilePathOrNull() }

    private fun extractAudioUrl(attachment: ChatAttachment?): String? = attachment?.getFilePathOrNull()

    private fun createAssistantPlaceholder(timestampMs: Long): LocalMessage =
        LocalMessage(
            localId = "local-assistant-$timestampMs",
            role = ConversationMessageRole.ASSISTANT,
            content = null,
            messageType = ChatMessageType.TEXT,
            createdAtMs = timestampMs + 1,
            status = LocalMessageStatus.WAITING,
            isPlaceholder = true,
            draftForRetry = null,
        )

    private fun buildSentMessages(
        userMessage: ChatMessage,
        assistantMessage: ChatMessage?,
    ): List<SentMessage> =
        buildList {
            parseTimestampToEpochMs(userMessage.createdAt)?.let {
                add(SentMessage(insertedAtMs = it, message = userMessage))
            }
            assistantMessage?.let { message ->
                parseTimestampToEpochMs(message.createdAt)?.let {
                    add(SentMessage(insertedAtMs = it, message = message))
                }
            }
        }

    private fun handleSendSuccess(
        result: SendMessageResult,
        userLocalId: String,
        assistantLocalId: String,
    ) {
        val sentMessages = buildSentMessages(result.userMessage, result.assistantMessage)
        _overlay.update { state ->
            val remainingPending =
                state.pending.filterNot { it.localId == userLocalId || it.localId == assistantLocalId }
            state.copy(
                pending = remainingPending,
                sent = state.sent + sentMessages,
            )
        }
    }

    private fun handleSendFailure(
        error: Throwable,
        userLocalId: String,
        assistantLocalId: String,
    ) {
        _overlay.update { state ->
            val updatedPending =
                state.pending.mapNotNull { msg ->
                    when (msg.localId) {
                        assistantLocalId -> null
                        userLocalId ->
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

    @OptIn(ExperimentalTime::class)
    fun sendMessage(draft: SendMessageDraft) {
        val convId = conversationId ?: return

        val now = Clock.System.now().toEpochMilliseconds()
        val localUserId = "local-user-$now"
        val localAssistantId = "local-assistant-$now"

        val userLocal =
            LocalMessage(
                localId = localUserId,
                role = ConversationMessageRole.USER,
                content = draft.content,
                messageType = draft.messageType,
                mediaUrls = extractMediaUrls(draft.mediaAttachments),
                audioUrl = extractAudioUrl(draft.audioAttachment),
                audioDurationSeconds = draft.audioDurationSeconds,
                createdAtMs = now,
                status = LocalMessageStatus.SENDING,
                isPlaceholder = false,
                draftForRetry = draft,
            )
        val assistantPlaceholder = createAssistantPlaceholder(now)

        _overlay.update { it.copy(pending = it.pending + userLocal + assistantPlaceholder) }

        viewModelScope.launch {
            sendMessageUseCase(
                SendMessageUseCase.Params(
                    conversationId = convId,
                    draft = draft,
                ),
            ).onSuccess { result ->
                handleSendSuccess(result, localUserId, localAssistantId)
            }.onFailure { error ->
                handleSendFailure(error, localUserId, localAssistantId)
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    fun retry(localUserMessageId: String) {
        val convId = conversationId ?: return
        _overlay
            .value
            .pending
            .firstOrNull { it.localId == localUserMessageId }
            ?.draftForRetry
            ?.let { draft ->
                val now = Clock.System.now().toEpochMilliseconds()
                val localAssistantId = "local-assistant-$now"
                val assistantPlaceholder = createAssistantPlaceholder(now)

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
                        handleSendSuccess(result, localUserMessageId, localAssistantId)
                    }.onFailure { error ->
                        handleSendFailure(error, localUserMessageId, localAssistantId)
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

    companion object {
        private const val PAGE_SIZE = 10
        private const val PREFETCH_DISTANCE = 5
        const val TEST_INFLUENCER_ID = "qg2pi-g3xl4-uprdd-macwr-64q7r-plotv-xm3bg-iayu3-rnpux-7ikkz-hqe"
    }
}

data class ConversationViewState(
    val isCreating: Boolean = false,
    val isDeleting: Boolean = false,
    val error: String? = null,
    val conversationId: String? = null,
    val influencer: ConversationInfluencer? = null,
)

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
    val mediaUrls: List<String> = emptyList(),
    val audioUrl: String? = null,
    val audioDurationSeconds: Int? = null,
    val createdAtMs: Long,
    val status: LocalMessageStatus,
    val isPlaceholder: Boolean,
    val errorMessage: String? = null,
    val draftForRetry: SendMessageDraft?,
)
