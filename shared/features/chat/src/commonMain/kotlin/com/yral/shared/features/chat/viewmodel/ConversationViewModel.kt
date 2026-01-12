package com.yral.shared.features.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.coroutines.runSuspendCatching
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.shared.analytics.events.InfluencerSource
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.core.session.SessionManager
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.features.chat.analytics.ChatTelemetry
import com.yral.shared.features.chat.attachments.ChatAttachment
import com.yral.shared.features.chat.attachments.FilePathChatAttachment
import com.yral.shared.features.chat.domain.ChatRepository
import com.yral.shared.features.chat.domain.ConversationMessagesPagingSource
import com.yral.shared.features.chat.domain.EmptyMessagesPagingSource
import com.yral.shared.features.chat.domain.models.ChatMessage
import com.yral.shared.features.chat.domain.models.ChatMessageType
import com.yral.shared.features.chat.domain.models.ConversationInfluencer
import com.yral.shared.features.chat.domain.models.ConversationMessageRole
import com.yral.shared.features.chat.domain.models.SendMessageDraft
import com.yral.shared.features.chat.domain.models.SendMessageResult
import com.yral.shared.features.chat.domain.usecases.CreateConversationUseCase
import com.yral.shared.features.chat.domain.usecases.DeleteConversationUseCase
import com.yral.shared.features.chat.domain.usecases.SendMessageUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.routing.deeplink.engine.UrlBuilder
import com.yral.shared.libs.routing.routes.api.UserProfileRoute
import com.yral.shared.libs.sharing.LinkGenerator
import com.yral.shared.libs.sharing.LinkInput
import com.yral.shared.libs.sharing.ShareService
import com.yral.shared.rust.service.utils.getUserInfoServiceCanister
import com.yral.shared.rust.service.utils.propicFromPrincipal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import yral_mobile.shared.libs.designsystem.generated.resources.msg_profile_share
import yral_mobile.shared.libs.designsystem.generated.resources.msg_profile_share_desc
import yral_mobile.shared.libs.designsystem.generated.resources.profile_share_default_name
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@Suppress("LongParameterList")
class ConversationViewModel(
    private val chatRepository: ChatRepository,
    private val useCaseFailureListener: UseCaseFailureListener,
    private val sendMessageUseCase: SendMessageUseCase,
    private val createConversationUseCase: CreateConversationUseCase,
    private val deleteConversationUseCase: DeleteConversationUseCase,
    private val shareService: ShareService,
    private val urlBuilder: UrlBuilder,
    private val linkGenerator: LinkGenerator,
    private val crashlyticsManager: CrashlyticsManager,
    private val sessionManager: SessionManager,
    private val chatTelemetry: ChatTelemetry,
) : ViewModel() {
    private var influencerSource: InfluencerSource = InfluencerSource.CARD

    /**
     * Message ordering:
     * - Network paging uses `order=desc` (latest-first)
     * - UI uses `LazyColumn(reverseLayout = true)` to display oldest at top, newest at bottom
     * - Render overlay first, then history
     */
    private val _viewState = MutableStateFlow(ConversationViewState())
    val viewState: StateFlow<ConversationViewState> = _viewState.asStateFlow()

    private val conversationId: String?
        get() = _viewState.value.conversationId
    private val loadedMessageIds = MutableStateFlow<Set<String>>(emptySet())
    private val initialOffset = MutableStateFlow<Int?>(null)

    private val pagingConfig =
        PagingConfig(
            pageSize = PAGE_SIZE,
            initialLoadSize = PAGE_SIZE,
            prefetchDistance = PREFETCH_DISTANCE,
            enablePlaceholders = false,
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val pagedHistory: Flow<PagingData<ChatMessage>> =
        _viewState
            .map { it.conversationId to it.paginatedHistoryAvailable }
            .distinctUntilChanged()
            .flatMapLatest { (convId, hasMoreMessages) ->
                if (convId.isNullOrBlank() || !hasMoreMessages) {
                    Pager(
                        config = pagingConfig,
                        pagingSourceFactory = { EmptyMessagesPagingSource() },
                    ).flow
                } else {
                    loadedMessageIds.value = emptySet()
                    val offset =
                        initialOffset.value
                            .takeIf { initialOffset.value != null }
                            .also { initialOffset.value = null }
                    Pager(
                        config = pagingConfig,
                        pagingSourceFactory = {
                            ConversationMessagesPagingSource(
                                conversationId = convId,
                                chatRepository = chatRepository,
                                useCaseFailureListener = useCaseFailureListener,
                                initialOffset = offset,
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
        viewModelScope.launch {
            sessionManager
                .observeSessionPropertyWithDefault(
                    selector = { it.isSocialSignIn },
                    defaultValue = false,
                ).collect { isSocialSignIn ->
                    resetState()
                    _viewState.update { it.copy(isSocialSignedIn = isSocialSignIn) }
                }
        }
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
        messageCount: Int,
    ) {
        val previousConversationId = _viewState.value.conversationId

        if (previousConversationId != id) {
            _overlay.value = OverlayState()
        }

        // Merge incoming influencer with any existing data to preserve category from the wall
        val existingInfluencer = _viewState.value.influencer
        val resolvedInfluencer =
            when {
                influencer == null -> existingInfluencer
                existingInfluencer == null -> influencer
                else ->
                    influencer.copy(
                        category = influencer.category.ifBlank { existingInfluencer.category },
                        displayName =
                            influencer.displayName.ifBlank {
                                existingInfluencer.displayName
                            },
                        name = influencer.name.ifBlank { existingInfluencer.name },
                        avatarUrl = influencer.avatarUrl.ifBlank { existingInfluencer.avatarUrl },
                        suggestedMessages =
                            if (influencer.suggestedMessages.isNotEmpty()) {
                                influencer.suggestedMessages
                            } else {
                                existingInfluencer.suggestedMessages
                            },
                    )
            }

        val sentMessages =
            recentMessages.mapNotNull { message ->
                parseTimestampToEpochMs(message.createdAt)?.let { timestampMs ->
                    SentMessage(insertedAtMs = timestampMs, message = message)
                }
            }
        _overlay.update { it.copy(sent = it.sent + sentMessages) }

        val paginatedHistoryAvailable = messageCount > PAGE_SIZE

        if (recentMessages.isNotEmpty() && paginatedHistoryAvailable) {
            initialOffset.value = recentMessages.size
        }

        _viewState.update {
            it.copy(
                conversationId = id,
                influencer = resolvedInfluencer,
                paginatedHistoryAvailable = paginatedHistoryAvailable,
            )
        }

        if (resolvedInfluencer != null) {
            refreshShareCopy()
        }

        if (previousConversationId != id) {
            chatTelemetry.chatSessionStarted(
                influencerId = resolvedInfluencer?.id.orEmpty(),
                influencerType = resolvedInfluencer?.category.orEmpty(),
                chatSessionId = id,
                source = influencerSource,
            )
        }
    }

    private fun refreshShareCopy() {
        viewModelScope.launch {
            val influencer = _viewState.value.influencer
            val displayName =
                influencer?.displayName?.takeIf { it.isNotBlank() }
                    ?: influencer?.name
                    ?: getString(DesignRes.string.profile_share_default_name)
            val message = getString(DesignRes.string.msg_profile_share, displayName)
            val description = getString(DesignRes.string.msg_profile_share_desc, displayName)
            _viewState.update { current ->
                current.copy(
                    shareDisplayName = displayName,
                    shareMessage = message,
                    shareDescription = description,
                )
            }
        }
    }

    fun initializeForInfluencer(
        influencerId: String,
        influencerCategory: String,
        influencerSource: InfluencerSource = InfluencerSource.CARD,
    ) {
        this.influencerSource = influencerSource
        val currentInfluencerId = _viewState.value.influencer?.id
        val currentConversationId = _viewState.value.conversationId
        // same influencer and conversation exists
        if (currentInfluencerId == influencerId && currentConversationId != null) {
            return
        }
        // current influencer is different from influencerId
        if (currentInfluencerId != null && currentInfluencerId != influencerId) {
            resetState()
        }
        if (_viewState.value.conversationId == null) {
            _viewState.update {
                it.copy(
                    influencer =
                        ConversationInfluencer(
                            id = influencerId,
                            name = "",
                            displayName = "",
                            avatarUrl = "",
                            category = influencerCategory,
                        ),
                )
            }
            createConversation(influencerId)
        }
    }

    private fun resetState() {
        val isSocialSignedIn = _viewState.value.isSocialSignedIn
        _viewState.update {
            ConversationViewState(
                isCreating = false,
                isDeleting = false,
                error = null,
                conversationId = null,
                influencer = null,
                paginatedHistoryAvailable = false,
                shareDisplayName = "",
                shareMessage = "",
                shareDescription = "",
                isSocialSignedIn = isSocialSignedIn,
            )
        }
        _overlay.value = OverlayState()
        loadedMessageIds.value = emptySet()
        initialOffset.value = null
    }

    private fun createConversation(influencerId: String) {
        _viewState.update { it.copy(isCreating = true, error = null) }
        viewModelScope.launch {
            createConversationUseCase(CreateConversationUseCase.Params(influencerId = influencerId))
                .onSuccess { conversation ->
                    setConversationId(
                        id = conversation.id,
                        influencer = conversation.influencer,
                        recentMessages = conversation.recentMessages,
                        messageCount = conversation.messageCount,
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
                    // Reset state after successful deletion, then create a new conversation
                    resetState()
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

    fun shareProfile() {
        val influencer = _viewState.value.influencer ?: return
        val principal = influencer.id
        if (principal.isBlank()) return

        viewModelScope.launch {
            val canisterId = getUserInfoServiceCanister()
            val route =
                UserProfileRoute(
                    canisterId = canisterId,
                    userPrincipalId = principal,
                    profilePic = influencer.avatarUrl.takeIf { it.isNotBlank() },
                    username = influencer.displayName.takeIf { it.isNotBlank() } ?: influencer.name,
                    isFromServiceCanister = true,
                )
            val internalUrl = urlBuilder.build(route) ?: return@launch
            runSuspendCatching {
                val imageUrl =
                    influencer.avatarUrl
                        .takeIf { it.isNotBlank() }
                        ?: propicFromPrincipal(principal)
                val shareState = _viewState.value
                val link =
                    linkGenerator.generateShareLink(
                        LinkInput(
                            internalUrl = internalUrl,
                            title = shareState.shareMessage,
                            description = shareState.shareDescription,
                            feature = "share_profile",
                            tags = listOf("organic", "profile_share"),
                            contentImageUrl = imageUrl,
                            metadata = mapOf("user_principal_id" to principal),
                        ),
                    )
                val text = "${shareState.shareMessage} $link"
                shareService.shareImageWithText(
                    imageUrl = imageUrl,
                    text = text,
                )
            }.onFailure {
                Logger.e(ConversationViewModel::class.simpleName!!, it) { "Failed to share profile" }
                crashlyticsManager.recordException(
                    YralException(it),
                    ExceptionType.DEEPLINK,
                )
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

    @OptIn(ExperimentalTime::class)
    private fun handleSendSuccess(
        result: SendMessageResult,
        userLocalId: String,
        assistantLocalId: String,
        sentAtMs: Long?,
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

        val convId = conversationId ?: return
        val influencer = _viewState.value.influencer ?: return
        val response = result.assistantMessage?.content.orEmpty()
        val responseLength = response.length
        val responseLatencyMs =
            sentAtMs?.let { start ->
                (Clock.System.now().toEpochMilliseconds() - start).coerceAtLeast(0)
            } ?: 0
        chatTelemetry.aiMessageDelivered(
            influencerId = influencer.id,
            influencerType = influencer.category,
            chatSessionId = convId,
            responseLatencyMs = responseLatencyMs.toInt(),
            responseLength = responseLength,
            message = response,
        )
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

        _viewState.value.influencer?.let { influencer ->
            chatTelemetry.userMessageSent(
                influencerId = influencer.id,
                influencerType = influencer.category,
                chatSessionId = convId,
                messageLength = draft.content?.length ?: 0,
                messageType = draft.messageType.name.lowercase(),
                message = draft.content.orEmpty(),
            )
        }

        viewModelScope.launch {
            sendMessageUseCase(
                SendMessageUseCase.Params(
                    conversationId = convId,
                    draft = draft,
                ),
            ).onSuccess { result ->
                handleSendSuccess(result, localUserId, localAssistantId, now)
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
                        handleSendSuccess(result, localUserMessageId, localAssistantId, now)
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
    }
}

data class ConversationViewState(
    val isCreating: Boolean = false,
    val isDeleting: Boolean = false,
    val error: String? = null,
    val conversationId: String? = null,
    val influencer: ConversationInfluencer? = null,
    val paginatedHistoryAvailable: Boolean = false,
    val shareDisplayName: String = "",
    val shareMessage: String = "",
    val shareDescription: String = "",
    val isSocialSignedIn: Boolean = false,
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
