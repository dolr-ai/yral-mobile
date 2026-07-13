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
import com.yral.featureflag.AppFeatureFlags
import com.yral.featureflag.ChatFeatureFlags
import com.yral.featureflag.FeatureFlagManager
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.core.session.SessionManager
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.data.domain.models.ConversationInfluencerSource
import com.yral.shared.features.chat.analytics.ChatTelemetry
import com.yral.shared.features.chat.attachments.ChatAttachment
import com.yral.shared.features.chat.attachments.FilePathChatAttachment
import com.yral.shared.features.chat.data.CollageCache
import com.yral.shared.features.chat.data.ConversationContentCache
import com.yral.shared.features.chat.domain.BotSubscriptionCatalog
import com.yral.shared.features.chat.domain.ChatErrorMapper
import com.yral.shared.features.chat.domain.ChatRepository
import com.yral.shared.features.chat.domain.ConversationMessagesPagingSource
import com.yral.shared.features.chat.domain.EmptyMessagesPagingSource
import com.yral.shared.features.chat.domain.httpStatusOrNull
import com.yral.shared.features.chat.domain.models.AssistantError
import com.yral.shared.features.chat.domain.models.AssistantErrorCode
import com.yral.shared.features.chat.domain.models.AssistantErrorPresentation
import com.yral.shared.features.chat.domain.models.ChatError
import com.yral.shared.features.chat.domain.models.ChatMessage
import com.yral.shared.features.chat.domain.models.ChatMessageType
import com.yral.shared.features.chat.domain.models.Collage
import com.yral.shared.features.chat.domain.models.ConversationInfluencer
import com.yral.shared.features.chat.domain.models.ConversationMessageRole
import com.yral.shared.features.chat.domain.models.GrantError
import com.yral.shared.features.chat.domain.models.SendMessageDraft
import com.yral.shared.features.chat.domain.models.SendMessageResult
import com.yral.shared.features.chat.domain.models.StreamEvent
import com.yral.shared.features.chat.domain.usecases.CheckChatAccessUseCase
import com.yral.shared.features.chat.domain.usecases.CreateConversationUseCase
import com.yral.shared.features.chat.domain.usecases.DeleteConversationUseCase
import com.yral.shared.features.chat.domain.usecases.GetHumanCreatorTakeoverStatusUseCase
import com.yral.shared.features.chat.domain.usecases.GetInfluencerCollageUseCase
import com.yral.shared.features.chat.domain.usecases.GrantChatAccessParams
import com.yral.shared.features.chat.domain.usecases.GrantChatAccessUseCase
import com.yral.shared.features.chat.domain.usecases.MarkConversationAsReadUseCase
import com.yral.shared.features.chat.domain.usecases.ReleaseHumanCreatorTakeoverUseCase
import com.yral.shared.features.chat.domain.usecases.RequestInfluencerImagesUseCase
import com.yral.shared.features.chat.domain.usecases.SendHumanCreatorMessageUseCase
import com.yral.shared.features.chat.domain.usecases.SendHumanMessageUseCase
import com.yral.shared.features.chat.domain.usecases.SendMessageUseCase
import com.yral.shared.features.chat.domain.usecases.StartHumanCreatorTakeoverUseCase
import com.yral.shared.features.chat.ui.conversation.shouldRenderAsMarkdown
import com.yral.shared.features.subscriptions.domain.FetchProductsUseCase
import com.yral.shared.iap.IAPManager
import com.yral.shared.iap.core.IAPError
import com.yral.shared.iap.core.model.ProductId
import com.yral.shared.iap.core.model.ProductType
import com.yral.shared.iap.core.model.Purchase
import com.yral.shared.iap.core.model.PurchaseState
import com.yral.shared.iap.utils.PurchaseContext
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.designsystem.component.toast.ToastStatus
import com.yral.shared.libs.routing.deeplink.engine.UrlBuilder
import com.yral.shared.libs.routing.routes.api.UserProfileRoute
import com.yral.shared.libs.sharing.LinkGenerator
import com.yral.shared.libs.sharing.LinkInput
import com.yral.shared.libs.sharing.ShareService
import com.yral.shared.rust.service.utils.getUserInfoServiceCanister
import com.yral.shared.rust.service.utils.propicFromPrincipal
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.getString
import yral_mobile.shared.features.chat.generated.resources.Res
import yral_mobile.shared.features.chat.generated.resources.assistant_error_connection_timed_out
import yral_mobile.shared.features.chat.generated.resources.collage_quota_used_toast
import yral_mobile.shared.features.chat.generated.resources.collage_request_failed_toast
import yral_mobile.shared.features.chat.generated.resources.influencer_subscription_purchase_failed
import yral_mobile.shared.features.chat.generated.resources.influencer_subscription_purchase_pending
import yral_mobile.shared.features.chat.generated.resources.influencer_subscription_purchase_unavailable
import yral_mobile.shared.features.chat.generated.resources.influencer_subscription_unlocked_toast
import yral_mobile.shared.features.chat.generated.resources.influencer_subscription_verification_pending
import yral_mobile.shared.features.chat.generated.resources.request_image_message
import yral_mobile.shared.libs.designsystem.generated.resources.msg_profile_share
import yral_mobile.shared.libs.designsystem.generated.resources.msg_profile_share_desc
import yral_mobile.shared.libs.designsystem.generated.resources.profile_share_default_name
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@OptIn(FlowPreview::class)
@Suppress("LongParameterList", "TooManyFunctions", "LargeClass")
class ConversationViewModel(
    flagManager: FeatureFlagManager,
    private val chatRepository: ChatRepository,
    private val useCaseFailureListener: UseCaseFailureListener,
    private val sendMessageUseCase: SendMessageUseCase,
    private val sendHumanMessageUseCase: SendHumanMessageUseCase,
    private val createConversationUseCase: CreateConversationUseCase,
    private val deleteConversationUseCase: DeleteConversationUseCase,
    private val markConversationAsReadUseCase: MarkConversationAsReadUseCase,
    private val shareService: ShareService,
    private val urlBuilder: UrlBuilder,
    private val linkGenerator: LinkGenerator,
    private val crashlyticsManager: CrashlyticsManager,
    private val sessionManager: SessionManager,
    private val chatTelemetry: ChatTelemetry,
    private val chatErrorMapper: ChatErrorMapper,
    private val iapManager: IAPManager,
    private val fetchProductsUseCase: FetchProductsUseCase,
    private val checkChatAccessUseCase: CheckChatAccessUseCase,
    private val grantChatAccessUseCase: GrantChatAccessUseCase,
    private val chatUnreadRefreshSignal: ChatUnreadRefreshSignal,
    private val startHumanCreatorTakeoverUseCase: StartHumanCreatorTakeoverUseCase,
    private val releaseHumanCreatorTakeoverUseCase: ReleaseHumanCreatorTakeoverUseCase,
    private val sendHumanCreatorMessageUseCase: SendHumanCreatorMessageUseCase,
    private val getHumanCreatorTakeoverStatusUseCase: GetHumanCreatorTakeoverStatusUseCase,
    private val conversationContentCache: ConversationContentCache,
    private val requestInfluencerImagesUseCase: RequestInfluencerImagesUseCase,
    private val getInfluencerCollageUseCase: GetInfluencerCollageUseCase,
    private val collageCache: CollageCache,
) : ViewModel() {
    /**
     * Message ordering:
     * - Network paging uses `order=desc` (latest-first)
     * - UI uses `LazyColumn(reverseLayout = true)` to display oldest at top, newest at bottom
     * - Render overlay first, then history
     */
    private val _viewState =
        MutableStateFlow(
            ConversationViewState(
                loginPromptMessageThreshold = flagManager.get(ChatFeatureFlags.Chat.LoginPromptMessageThreshold),
                requireAuthBeforeFirstSend = flagManager.isEnabled(ChatFeatureFlags.Chat.RequireAuthBeforeFirstSend),
                subscriptionMandatoryThreshold = flagManager.get(ChatFeatureFlags.Chat.SubscriptionMandatoryThreshold),
                isSubscriptionEnabled = flagManager.isEnabled(AppFeatureFlags.Common.EnableSubscription),
                isChatAsHumanCreatorEnabled = flagManager.get(ChatFeatureFlags.Chat.ChatAsHumanCreatorEnabled),
                isSseStreamingEnabled = flagManager.get(ChatFeatureFlags.Chat.SseStreamingEnabled),
                isAudioRecordingEnabled = flagManager.get(ChatFeatureFlags.Chat.AudioRecordingEnabled),
            ),
        )
    val viewState: StateFlow<ConversationViewState> = _viewState.asStateFlow()

    private val influencerSubscriptionToastChannel = Channel<InfluencerSubscriptionToastEvent>(Channel.CONFLATED)
    val influencerSubscriptionToastFlow = influencerSubscriptionToastChannel.receiveAsFlow()

    private val conversationId: String?
        get() = _viewState.value.conversationId
    private val loadedMessageIds = MutableStateFlow<Set<String>>(emptySet())
    private val initialOffset = MutableStateFlow<Int?>(null)
    private val historyRefreshTrigger = MutableStateFlow(0)

    private val pagingConfig =
        PagingConfig(
            pageSize = PAGE_SIZE,
            initialLoadSize = PAGE_SIZE,
            prefetchDistance = PREFETCH_DISTANCE,
            enablePlaceholders = false,
        )

    fun refreshHistory() {
        historyRefreshTrigger.update { it + 1 }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val pagedHistory: Flow<PagingData<ChatMessage>> =
        combine(
            _viewState.map { it.conversationId to it.paginatedHistoryAvailable },
            historyRefreshTrigger,
        ) { (convId, hasMoreMessages), refreshCount -> Triple(convId, hasMoreMessages, refreshCount) }
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
                    Logger.i("SSE") { "paging-loadedIds += ${message.id} (size=${currentIds.size + 1})" }
                    loadedMessageIds.update { it + message.id }
                }
                ConversationMessageItem.Remote(message) as ConversationMessageItem
            }
        }

    private val _overlay = MutableStateFlow(OverlayState())

    // Phase 5b: server message id → Markdown-locked Boolean. Populated on SSE `done`
    // from the streaming Local's useMarkdownLocked. The screen consumes this to
    // override the per-Remote `shouldRenderAsMarkdown` decision, so the Markdown/Text
    // path stays identical across the Local→Remote swap. ViewModel-scoped only; on
    // re-entry the map is empty and the default decision kicks in (which is
    // identical for stable content).
    private val _streamMarkdownLockedRemoteIds = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val streamMarkdownLockedRemoteIds: StateFlow<Map<String, Boolean>> =
        _streamMarkdownLockedRemoteIds.asStateFlow()

    // Phase 6: the latest assistant-side error for the current conversation, paired
    // with the draft that produced it so the AssistantErrorBubble's retry tap can
    // re-run the stream without scraping state out of the overlay. Cleared on the
    // first successful token of a retry, on a fresh send, and on conversation
    // switch — at most one error visible at a time.
    private val _assistantError = MutableStateFlow<AssistantErrorPresentation?>(null)
    val assistantError: StateFlow<AssistantErrorPresentation?> = _assistantError.asStateFlow()

    // Phase 7-final: send button is disabled while an AI reply is in flight,
    // matching production chat-ai behavior (so users don't experience a UX
    // change at cutover). Covers both the SSE path (active stream) and the
    // legacy non-streaming path (sendMessageUseCase in flight). Implemented as
    // a reference count rather than a Boolean so a Done that immediately
    // drains a queued send keeps the count > 0 across the transition and the
    // button doesn't flicker enabled-then-disabled.
    private val activeReplyCount = MutableStateFlow(0)
    val isReplyInProgress: StateFlow<Boolean> =
        activeReplyCount
            .map { it > 0 }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // Phase 7: single-stream invariant. At most one SSE collect is in flight per
    // conversation. New sends that arrive while a stream is active are buffered
    // in [pendingStreamingQueue] and drained when the active stream terminates
    // (Done, Failed, idle timeout, or cancellation). The user's USER Local is
    // still added to overlay immediately so they see their message instantly
    // — only the assistant placeholder + SSE collect waits its turn.
    private var activeStreamJob: Job? = null
    private val pendingStreamingQueue: ArrayDeque<QueuedStreamingSend> = ArrayDeque()

    // Phase 9 circuit breaker. Incremented on each terminal stream failure
    // (Failed event, idle timeout, or onFailure of the SSE collect itself).
    // Reset to 0 on Done. When the counter reaches CIRCUIT_BREAKER_THRESHOLD,
    // `shouldStream` returns false for the rest of the session so subsequent
    // sends silently take the non-streaming legacy path. There is no UI
    // exposure of this — it's a defense-in-depth fallback for sustained
    // backend SSE flakiness during the rollout.
    private var consecutiveStreamFailures: Int = 0

    private data class QueuedStreamingSend(
        val draft: SendMessageDraft,
        val userLocalId: String,
        val timestampMs: Long,
    )

    private val systemOverlayMessagesFlow = MutableStateFlow<List<SentMessage>>(emptyList())

    val overlay: StateFlow<List<ConversationMessageItem>> =
        combine(_overlay, loadedMessageIds, systemOverlayMessagesFlow) { overlayState, _, systemMessages ->
            // VM-level filter intentionally removed: the screen already dedups
            // overlay vs paged history via `isDuplicateOfOverlay`. Filtering
            // here pulled the newest sent messages OUT of overlay when the
            // cache+page sets overlapped, dropping them onto pagedItems —
            // which render at the visual TOP under reverseLayout=true and
            // appear "missing" from the bottom of the conversation.
            buildList {
                overlayState.pending.forEach { pending ->
                    add(pending.createdAtMs to ConversationMessageItem.Local(pending))
                }
                overlayState.sent.forEach { sent ->
                    add(sent.insertedAtMs to ConversationMessageItem.Remote(sent.message))
                }
                systemMessages.forEach { sent ->
                    add(sent.insertedAtMs to ConversationMessageItem.Remote(sent.message))
                }
            }.sortedWith(compareByDescending { it.first }).map { it.second }
        }.distinctUntilChanged()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(MESSAGES_STOP_TIMEOUT_MS),
                emptyList(),
            )

    // Phase 5b: paging snapshot pushed in from the screen via `recordHistorySnapshot`.
    // Combined with `_overlay.sent` to compose the cache write payload. Kept as
    // ChatMessage list (not ConversationMessageItem) because the cache stores raw
    // domain messages.
    private val lastHistorySnapshot = MutableStateFlow<List<ChatMessage>>(emptyList())

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
        // Phase 5b: debounced cache writer. Snapshots (conversationId, overlay.sent + history)
        // and writes to the ConversationContentCache whenever the visible message set
        // settles. The 500ms debounce coalesces token-by-token overlay churn into one
        // cache write per stable state.
        viewModelScope.launch {
            combine(
                _viewState.map { it.conversationId.orEmpty() }.distinctUntilChanged(),
                _overlay.map { it.sent.map { sent -> sent.message } },
                lastHistorySnapshot,
            ) { convId, sentMsgs, historyMsgs ->
                CacheSnapshot(convId, sentMsgs, historyMsgs)
            }.debounce(CACHE_WRITE_DEBOUNCE_MS)
                .collect { snap ->
                    if (snap.conversationId.isBlank()) return@collect
                    val merged = mergeForCache(snap.sentMessages, snap.historyMessages)
                    if (merged.isNotEmpty()) {
                        conversationContentCache.write(snap.conversationId, merged)
                    }
                }
        }
    }

    /** Phase 5b: screen pushes the LazyPagingItems snapshot here so the cache writer can include it. */
    fun recordHistorySnapshot(messages: List<ChatMessage>) {
        lastHistorySnapshot.value = messages
    }

    private data class CacheSnapshot(
        val conversationId: String,
        val sentMessages: List<ChatMessage>,
        val historyMessages: List<ChatMessage>,
    )

    /**
     * Dedup `sent` + `history` by message id (sent wins because it's the most recently
     * confirmed source). Return ordered by `createdAt` ascending so the cache stores a
     * chronological list — the reader passes them through `parseTimestampToEpochMs`
     * to re-derive the SentMessage insertion order on hydration.
     */
    @OptIn(ExperimentalTime::class)
    private fun mergeForCache(
        sent: List<ChatMessage>,
        history: List<ChatMessage>,
    ): List<ChatMessage> {
        if (sent.isEmpty() && history.isEmpty()) return emptyList()
        val byId = LinkedHashMap<String, ChatMessage>(sent.size + history.size)
        history.forEach { byId[it.id] = it }
        sent.forEach { byId[it.id] = it }
        return byId.values.sortedBy { parseTimestampToEpochMs(it.createdAt) ?: 0L }
    }

    @OptIn(ExperimentalTime::class)
    fun setSystemOverlayMessages(
        subscriptionCardMessage: String?,
        accessActivatedMessage: String?,
    ) {
        val convId =
            _viewState.value.conversationId ?: run {
                systemOverlayMessagesFlow.value = emptyList()
                return
            }
        val now = Clock.System.now().toEpochMilliseconds()
        val createdAt = Instant.fromEpochMilliseconds(now).toString()
        val list =
            when {
                accessActivatedMessage != null -> {
                    listOf(
                        createSystemSentMessage(
                            now,
                            convId,
                            createdAt,
                            "system-access-activated",
                            accessActivatedMessage,
                        ),
                    )
                }

                subscriptionCardMessage != null -> {
                    listOf(
                        createSystemSentMessage(
                            now,
                            convId,
                            createdAt,
                            "system-free-messages-over",
                            subscriptionCardMessage,
                        ),
                    )
                }

                else -> {
                    emptyList()
                }
            }
        systemOverlayMessagesFlow.value = list
    }

    private fun createSystemSentMessage(
        insertedAtMs: Long,
        conversationId: String,
        createdAt: String,
        messageId: String,
        content: String,
    ): SentMessage =
        SentMessage(
            insertedAtMs = insertedAtMs,
            message = createSystemChatMessage(messageId, conversationId, content, createdAt),
        )

    private fun createSystemChatMessage(
        id: String,
        conversationId: String,
        content: String,
        createdAt: String,
    ): ChatMessage =
        ChatMessage(
            id = id,
            conversationId = conversationId,
            role = ConversationMessageRole.ASSISTANT,
            content = content,
            messageType = ChatMessageType.TEXT,
            mediaUrls = emptyList(),
            audioUrl = null,
            audioDurationSeconds = null,
            tokenCount = null,
            createdAt = createdAt,
        )

    private fun updateInfluencerSubscriptionProductState(influencerId: String) {
        if (!_viewState.value.isSubscriptionEnabled) return
        Logger.d("SubscriptionX") { "checkChatAccess for influencer=$influencerId" }
        _viewState.update { it.copy(isChatAccessLoading = true) }
        viewModelScope.launch {
            checkChatAccessUseCase(influencerId)
                .onSuccess { status ->
                    Logger.d("SubscriptionX") {
                        "checkChatAccess result: hasAccess=${status.hasAccess}, expiresAtMs=${status.expiresAtMs}"
                    }
                    if (status.hasAccess) {
                        _viewState.update {
                            it.copy(
                                isInfluencerSubscriptionPurchasedAndVerified = true,
                                chatAccessExpiresAtMs = status.expiresAtMs,
                                isChatAccessLoading = false,
                            )
                        }
                    } else {
                        retryUngrantedChatAccess(influencerId)
                    }
                }.onFailure { error ->
                    Logger.e("SubscriptionX", error) { "checkChatAccess failed" }
                    _viewState.update { it.copy(isChatAccessLoading = false) }
                    fetchInfluencerSubscriptionProducts()
                }
        }
    }

    private suspend fun retryUngrantedChatAccess(botId: String) {
        val product = BotSubscriptionCatalog.chatProductFor(botId)
        runSuspendCatching {
            iapManager.restorePurchases()
        }.onSuccess { restoreResult ->
            val purchases = restoreResult.getOrNull()?.purchases.orEmpty()
            val ungrantedPurchase =
                purchases.firstOrNull { purchase ->
                    purchase.productId == product &&
                        purchase.state == PurchaseState.PURCHASED
                }

            if (ungrantedPurchase?.purchaseToken != null) {
                Logger.d("SubscriptionX") { "Found ungranted ${product.productId}, retrying grant..." }
                retryGrantAccess(botId, ungrantedPurchase, product.productId)
            } else {
                _viewState.update { it.copy(isChatAccessLoading = false) }
                fetchInfluencerSubscriptionProducts()
            }
        }.onFailure {
            Logger.e("SubscriptionX", it) { "Chat access restore failed" }
            _viewState.update { it.copy(isChatAccessLoading = false) }
            fetchInfluencerSubscriptionProducts()
        }
    }

    private suspend fun retryGrantAccess(
        botId: String,
        purchase: Purchase,
        productId: String,
    ) {
        val purchaseToken = checkNotNull(purchase.purchaseToken)
        grantChatAccessUseCase(
            GrantChatAccessParams(
                botId = botId,
                purchaseToken = purchaseToken,
                productId = productId,
            ),
        ).onSuccess { grantStatus ->
            chatTelemetry.subscriptionSuccess(botId)
            _viewState.update {
                it.copy(
                    isInfluencerSubscriptionPurchasedAndVerified = true,
                    chatAccessExpiresAtMs = grantStatus.expiresAtMs,
                    isChatAccessLoading = false,
                )
            }
        }.onFailure { error ->
            Logger.e("SubscriptionX", error) { "Grant retry failed for $productId" }
            chatTelemetry.subscriptionFailed(botId, "grant_retry_${error.message}")
            handleGrantFailure(error, purchaseToken, purchase.purchaseTime, productId)
        }
    }

    private suspend fun handleGrantFailure(
        error: Throwable,
        purchaseToken: String,
        purchaseTime: Long?,
        productId: String,
    ) {
        when (error) {
            is GrantError.ClientError -> {
                // 400: Token rejected (wrong bot, expired, cancelled). Consume to stop retry loop.
                // Consuming is a consumable-only Play API — a rejected bot
                // subscription token must be left alone (the store owns its
                // lifecycle; retry/self-heal happens via the verify endpoint).
                val isConsumable = ProductId.fromString(productId)?.productType == ProductType.ONE_TIME
                Logger.w("SubscriptionX") {
                    "Grant rejected (400): ${error.errorMsg}. ${if (isConsumable) "Consuming purchase." else ""}"
                }
                if (isConsumable) {
                    consumePurchaseInBackground(purchaseToken)
                }
                _viewState.update {
                    it.copy(
                        isInfluencerSubscriptionPurchasedAndVerified = false,
                        isChatAccessLoading = false,
                    )
                }
                fetchInfluencerSubscriptionProducts()
            }

            is GrantError.ServerError -> {
                // 5xx: Transient failure. Keep unconsumed for retry on next launch.
                Logger.w("SubscriptionX") { "Grant server error (${error.httpStatus}). Keeping purchase for retry." }
                handleInfluencerSubscriptionVerificationResult(
                    isPurchased = true,
                    expiresAtMs = purchaseTime?.let { it + FALLBACK_ACCESS_DURATION_MS },
                )
            }

            else -> {
                // Network error or unknown. Keep unconsumed for retry.
                Logger.w("SubscriptionX") { "Grant failed (network/unknown). Keeping purchase for retry." }
                handleInfluencerSubscriptionVerificationResult(
                    isPurchased = true,
                    expiresAtMs = purchaseTime?.let { it + FALLBACK_ACCESS_DURATION_MS },
                )
            }
        }
    }

    private fun fetchInfluencerSubscriptionProducts() {
        // Tara sells an auto-renewing bot_sub subscription; every other bot
        // sells the one-time daily_chat consumable.
        val product = BotSubscriptionCatalog.chatProductFor(_viewState.value.influencer?.id)
        viewModelScope.launch {
            crashlyticsManager.logMessage(
                "YRALIAP chat fetchProducts start ids=[${product.productId}] " +
                    "subEnabled=${_viewState.value.isSubscriptionEnabled} " +
                    "hasAccess=${_viewState.value.isInfluencerSubscriptionPurchasedAndVerified}",
            )
            fetchProductsUseCase(listOf(product))
                .onSuccess { products ->
                    val influencerAvailable = product.productId in products.map { it.id }.toSet()
                    val influencerOfferPrice =
                        products
                            .find { it.id == product.productId }
                            ?.let { p -> p.offerPrice.ifBlank { p.price } }
                    _viewState.update {
                        it.copy(
                            isInfluencerSubscriptionAvailableToPurchase = influencerAvailable,
                            influencerSubscriptionFormattedPrice = influencerOfferPrice,
                        )
                    }
                    crashlyticsManager.logMessage(
                        "YRALIAP chat fetchProducts success valid=${products.map { it.id }} " +
                            "${product.productId}Available=$influencerAvailable price=$influencerOfferPrice",
                    )
                }.onFailure {
                    crashlyticsManager.logMessage(
                        "YRALIAP chat fetchProducts failure error=${it::class.simpleName} message=${it.message}",
                    )
                    _viewState.update {
                        it.copy(
                            isInfluencerSubscriptionAvailableToPurchase = false,
                            influencerSubscriptionFormattedPrice = null,
                        )
                    }
                }
        }
    }

    fun trackFreeAccessExpired(botId: String) {
        chatTelemetry.freeAccessExpired(botId)
    }

    fun showPurchaseUnavailableToast() {
        viewModelScope.launch {
            influencerSubscriptionToastChannel.trySend(
                InfluencerSubscriptionToastEvent(
                    ToastStatus.Error,
                    getString(Res.string.influencer_subscription_purchase_unavailable),
                ),
            )
        }
    }

    fun launchInfluencerSubscriptionPurchase(purchaseContext: PurchaseContext?) {
        if (purchaseContext == null) {
            viewModelScope.launch {
                influencerSubscriptionToastChannel.trySend(
                    InfluencerSubscriptionToastEvent(
                        ToastStatus.Error,
                        getString(Res.string.influencer_subscription_purchase_unavailable),
                    ),
                )
            }
            return
        }
        if (!_viewState.value.isSubscriptionEnabled) return
        _viewState.value.influencer
            ?.id
            ?.let { chatTelemetry.subscriptionClicked(it) }
        _viewState.update { it.copy(isInfluencerSubscriptionPurchaseInProgress = true) }
        viewModelScope.launch {
            performInfluencerSubscriptionPurchase(purchaseContext, _viewState.value.influencer?.id)
        }
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    private suspend fun performInfluencerSubscriptionPurchase(
        purchaseContext: PurchaseContext,
        botId: String?,
    ) {
        val product = BotSubscriptionCatalog.chatProductFor(botId)
        // Check for a purchase the billing service doesn't know about yet
        // (failed grant / interrupted verify) before buying again.
        val existingPurchase = findUngrantedChatPurchase(product)
        if (existingPurchase != null && botId != null) {
            Logger.d("SubscriptionX") { "Found ungranted ${product.productId}, retrying grant instead of new purchase" }
            retryGrantAccess(botId, existingPurchase, product.productId)
            _viewState.update { it.copy(isInfluencerSubscriptionPurchaseInProgress = false) }
            return
        }

        runSuspendCatching {
            iapManager.purchaseProduct(
                productId = product,
                context = purchaseContext,
                acknowledgePurchase = false,
                verifyPurchase = false,
            )
        }.onSuccess { purchaseKotlinResult ->
            val purchase = purchaseKotlinResult.getOrNull()
            val purchaseToken = purchase?.purchaseToken
            val purchaseTime = purchase?.purchaseTime
            Logger.d("SubscriptionX") {
                "Purchase result: token=${purchaseToken != null}, time=$purchaseTime, state=${purchase?.state}"
            }
            if (purchase == null || purchaseToken == null) {
                botId?.let { chatTelemetry.subscriptionFailed(it, "no_purchase") }
                _viewState.update { it.copy(isInfluencerSubscriptionPurchaseInProgress = false) }
                return@onSuccess
            }
            if (botId != null) {
                Logger.d("SubscriptionX") { "Calling grantChatAccess for botId=$botId" }
                grantChatAccessUseCase(
                    GrantChatAccessParams(
                        botId = botId,
                        purchaseToken = purchaseToken,
                        productId = product.productId,
                    ),
                ).onSuccess { grantStatus ->
                    Logger.d("SubscriptionX") {
                        "Grant succeeded: hasAccess=${grantStatus.hasAccess}, expiresAtMs=${grantStatus.expiresAtMs}"
                    }
                    handleInfluencerSubscriptionVerificationResult(
                        isPurchased = true,
                        expiresAtMs = grantStatus.expiresAtMs,
                    )
                }.onFailure { grantError ->
                    Logger.e("SubscriptionX", grantError) { "Grant failed after purchase" }
                    botId.let { chatTelemetry.subscriptionFailed(it, "grant_${grantError.message}") }
                    handleGrantFailure(grantError, purchaseToken, purchaseTime, product.productId)
                }
            } else {
                Logger.w("SubscriptionX") { "No botId, using fallback 24h access" }
                handleInfluencerSubscriptionVerificationResult(
                    isPurchased = true,
                    expiresAtMs = purchaseTime?.let { t -> t + FALLBACK_ACCESS_DURATION_MS },
                )
            }
        }.onFailure { error ->
            _viewState.update { it.copy(isInfluencerSubscriptionPurchaseInProgress = false) }
            botId?.let { chatTelemetry.subscriptionFailed(it, error.message ?: "unknown") }
            when (error) {
                is IAPError.PurchaseCancelled -> {
                    return@onFailure
                }

                else -> {
                    val message =
                        when (error) {
                            is IAPError.PurchasePending -> {
                                getString(Res.string.influencer_subscription_purchase_pending)
                            }

                            else -> {
                                getString(Res.string.influencer_subscription_purchase_failed)
                            }
                        }
                    influencerSubscriptionToastChannel.trySend(
                        InfluencerSubscriptionToastEvent(ToastStatus.Error, message),
                    )
                }
            }
        }
    }

    private suspend fun handleInfluencerSubscriptionVerificationResult(
        isPurchased: Boolean,
        expiresAtMs: Long?,
    ) {
        _viewState.update {
            it.copy(
                isInfluencerSubscriptionPurchasedAndVerified = isPurchased,
                isInfluencerSubscriptionPurchaseInProgress = false,
                chatAccessExpiresAtMs = if (isPurchased) expiresAtMs else null,
            )
        }
        if (isPurchased) {
            _viewState.value.influencer
                ?.id
                ?.let { chatTelemetry.subscriptionSuccess(it) }
            val displayName =
                _viewState.value.influencer
                    ?.displayName
                    ?.takeIf { it.isNotBlank() }
                    ?: _viewState.value.influencer?.name
                    ?: getString(DesignRes.string.profile_share_default_name)
            val message = getString(Res.string.influencer_subscription_unlocked_toast, displayName)
            influencerSubscriptionToastChannel.trySend(
                InfluencerSubscriptionToastEvent(ToastStatus.Success, message),
            )
        } else {
            _viewState.value.influencer?.id?.let {
                chatTelemetry.subscriptionFailed(it, "verification_pending")
            }
            influencerSubscriptionToastChannel.trySend(
                InfluencerSubscriptionToastEvent(
                    ToastStatus.Info,
                    getString(Res.string.influencer_subscription_verification_pending),
                ),
            )
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
            // Phase 5b: stale-while-revalidate. Hydrate `_overlay.sent` from the
            // in-memory cache BEFORE the paging refresh fires, so the LazyColumn
            // shows the prior chat instantly instead of blanking for ~500ms while
            // page 0 fetches. Once paging completes, `loadedMessageIds` dedup
            // silently swaps the cached overlay copies for the paged ones.
            val cached = conversationContentCache.read(id)
            val hydratedSent =
                cached.mapNotNull { msg ->
                    parseTimestampToEpochMs(msg.createdAt)?.let { ts ->
                        SentMessage(insertedAtMs = ts, message = msg)
                    }
                }
            _overlay.value = OverlayState(sent = hydratedSent)
            // Phase 6: errors are scoped to the conversation that produced them.
            // Crossing the conversation boundary clears any in-flight error bubble.
            _assistantError.value = null
            // Collage state (including the request-today cooldown) is per-bot;
            // a stale cooldown must not disable Request Image for another bot.
            resetCollageState()
        }

        // Merge incoming influencer with any existing data to preserve category from the wall
        val existingInfluencer = _viewState.value.influencer
        val resolvedInfluencer =
            when {
                influencer == null -> {
                    existingInfluencer
                }

                existingInfluencer == null -> {
                    influencer
                }

                else -> {
                    influencer.copy(
                        category = influencer.category.ifBlank { existingInfluencer.category },
                        displayName =
                            influencer.displayName.ifBlank {
                                existingInfluencer.displayName
                            },
                        name = influencer.name.ifBlank { existingInfluencer.name },
                        avatarUrl = influencer.avatarUrl.ifBlank { existingInfluencer.avatarUrl },
                        suggestedMessages =
                            influencer
                                .suggestedMessages
                                .ifEmpty { existingInfluencer.suggestedMessages },
                    )
                }
            }

        val sentMessages =
            recentMessages.mapNotNull { message ->
                parseTimestampToEpochMs(message.createdAt)?.let { timestampMs ->
                    SentMessage(insertedAtMs = timestampMs, message = message)
                }
            }
        _overlay.update { it.copy(sent = it.sent + sentMessages) }

        val paginatedHistoryAvailable = messageCount > PAGE_SIZE

        // Audio-history-fix: always (re)assign initialOffset on every
        // setConversationId so a stale value from a previous conversation
        // visit can't leak through. Without this, an `initializeFromInbox`
        // call (which always passes recentMessages = emptyList()) leaves
        // initialOffset.value at whatever the previous conversation set it
        // to, so the new pager starts page 0 at a non-zero offset and
        // silently skips the most-recent messages — reproducing as
        // "voice message + AI reply missing after inbox re-entry".
        initialOffset.value =
            if (recentMessages.isNotEmpty() && paginatedHistoryAvailable) {
                recentMessages.size
            } else {
                null
            }

        _viewState.update {
            it.copy(
                conversationId = id,
                influencer = resolvedInfluencer,
                paginatedHistoryAvailable = paginatedHistoryAvailable,
                totalHistoryMessageCount = messageCount,
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
                source = _viewState.value.influencerSource,
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

    fun initializeFromInbox(
        conversationId: String,
        influencerId: String,
        influencerCategory: String,
        influencerSource: ConversationInfluencerSource,
        displayName: String? = null,
        userName: String? = null,
        avatarUrl: String? = null,
        // H2H: the other-user's principal_id when this conversation came
        // from the profile-screen Send Message tap. Null for AI chats.
        // When non-null we also skip the IAP/access-check cascade since
        // H2H has no subscription product.
        participantPrincipalId: String? = null,
    ) {
        val isBotAccount = sessionManager.isBotAccount == true
        val viewerPrincipal = sessionManager.userPrincipal
        _viewState.update {
            it.copy(
                influencerSource = influencerSource,
                isBotAccount = isBotAccount,
                currentUserPrincipalId = viewerPrincipal,
            )
        }
        val currentConversationId = _viewState.value.conversationId
        if (currentConversationId == conversationId) {
            // Same conversation reopened — participantPrincipalId may need a
            // refresh (e.g. nav-stack carrying a different value), so update
            // it without resetting anything else.
            _viewState.update { it.copy(participantPrincipalId = participantPrincipalId) }
            return
        }
        if (_viewState.value.conversationId != null && _viewState.value.conversationId != conversationId) {
            resetState()
        }
        setConversationId(
            id = conversationId,
            influencer =
                ConversationInfluencer(
                    id = influencerId,
                    name = userName.orEmpty(),
                    displayName = displayName.orEmpty(),
                    avatarUrl = avatarUrl.orEmpty(),
                    category = influencerCategory,
                ),
            recentMessages = emptyList(),
            messageCount = PAGE_SIZE + 1,
        )
        // Apply participantPrincipalId AFTER resetState/setConversationId so
        // resetState's fresh ConversationViewState doesn't blow the H2H
        // discriminator away. Pre-reset assignment was silently dropped on
        // every conversation switch, manifesting as Subscribe + 3-dot menu
        // appearing on H2H chats opened after browsing any other chat first.
        _viewState.update { it.copy(participantPrincipalId = participantPrincipalId) }
        // N1 gate: single call-site for the entire IAP cascade
        // (checkChatAccessUseCase → retryUngrantedChatAccess →
        // fetchInfluencerSubscriptionProducts). All four fallback entry
        // points reach back through updateInfluencerSubscriptionProductState
        // so this one skip covers them all.
        if (participantPrincipalId == null) {
            updateInfluencerSubscriptionProductState(influencerId)
        }
    }

    fun initializeForChatWall(
        influencerId: String,
        influencerCategory: String,
        influencerSource: ConversationInfluencerSource = ConversationInfluencerSource.CARD,
        displayName: String? = null,
        userName: String? = null,
        avatarUrl: String? = null,
    ) {
        val isBotAccount = sessionManager.isBotAccount == true
        val viewerPrincipal = sessionManager.userPrincipal
        _viewState.update {
            it.copy(
                influencerSource = influencerSource,
                isBotAccount = isBotAccount,
                currentUserPrincipalId = viewerPrincipal,
            )
        }
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
                            name = userName.orEmpty(),
                            displayName = displayName.orEmpty(),
                            avatarUrl = avatarUrl.orEmpty(),
                            category = influencerCategory,
                        ),
                )
            }
            updateInfluencerSubscriptionProductState(influencerId)
            createConversation(influencerId)
        }
    }

    private fun resetState() {
        val current = _viewState.value
        _viewState.update {
            ConversationViewState(
                isCreating = false,
                isDeleting = false,
                chatError = null,
                conversationId = null,
                influencer = null,
                isBotAccount = current.isBotAccount,
                paginatedHistoryAvailable = false,
                shareDisplayName = "",
                shareMessage = "",
                shareDescription = "",
                isSocialSignedIn = current.isSocialSignedIn,
                influencerSource = current.influencerSource,
                loginPromptMessageThreshold = current.loginPromptMessageThreshold,
                requireAuthBeforeFirstSend = current.requireAuthBeforeFirstSend,
                subscriptionMandatoryThreshold = current.subscriptionMandatoryThreshold,
                isSubscriptionEnabled = current.isSubscriptionEnabled,
                isChatAsHumanCreatorEnabled = current.isChatAsHumanCreatorEnabled,
                isSseStreamingEnabled = current.isSseStreamingEnabled,
                isAudioRecordingEnabled = current.isAudioRecordingEnabled,
                isInfluencerSubscriptionPurchasedAndVerified = false,
                isInfluencerSubscriptionAvailableToPurchase = current.isInfluencerSubscriptionAvailableToPurchase,
                isInfluencerSubscriptionPurchaseInProgress = false,
                influencerSubscriptionFormattedPrice = current.influencerSubscriptionFormattedPrice,
                totalHistoryMessageCount = 0,
            )
        }
        _overlay.value = OverlayState()
        systemOverlayMessagesFlow.value = emptyList()
        loadedMessageIds.value = emptySet()
        initialOffset.value = null
        historyRefreshTrigger.value = 0
        // Cross-conv leak fix: clear paged-history snapshot so the debounced
        // cache writer can't compose (new convId, stale prev-conv history)
        // and poison the new conversation's cache before its own paging arrives.
        lastHistorySnapshot.value = emptyList()
        stopTakeoverPolling()
        stopCountdownTicker()
        // Phase 7: tear down any in-flight stream and drop queued sends. The
        // stream's finally block sees the cancellation as structured-concurrency,
        // NOT the idle-timeout sentinel, so it won't synthesize a TRANSIENT error
        // on top of a conversation switch.
        activeStreamJob?.cancel()
        activeStreamJob = null
        pendingStreamingQueue.clear()
        _assistantError.value = null
        // Phase 9: each conversation gets a fresh circuit-breaker budget.
        consecutiveStreamFailures = 0
    }

    private fun createConversation(influencerId: String) {
        _viewState.update { it.copy(isCreating = true, chatError = null) }
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
                }.onFailure { throwable ->
                    val chatError =
                        chatErrorMapper.mapException(throwable) {
                            retryConversationCreation()
                        }
                    _viewState.update {
                        it.copy(
                            isCreating = false,
                            chatError = chatError,
                        )
                    }
                }
        }
    }

    fun deleteAndRecreateConversation(influencerId: String) {
        val currentConversationId = _viewState.value.conversationId ?: return
        _viewState.update { it.copy(isDeleting = true, chatError = null) }
        viewModelScope.launch {
            deleteConversationUseCase(DeleteConversationUseCase.Params(conversationId = currentConversationId))
                .onSuccess {
                    // Reset state after successful deletion, then create a new conversation
                    resetState()
                    createConversation(influencerId)
                    _viewState.update { it.copy(isDeleting = false) }
                }.onFailure { throwable ->
                    val chatError =
                        chatErrorMapper.mapException(throwable) {
                            retryDeleteAndRecreateConversation()
                        }
                    _viewState.update {
                        it.copy(
                            isDeleting = false,
                            isCreating = false,
                            chatError = chatError,
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

    private fun createStreamingAssistantPlaceholder(timestampMs: Long): LocalMessage =
        LocalMessage(
            localId = "local-streaming-assistant-$timestampMs",
            role = ConversationMessageRole.ASSISTANT,
            content = null,
            messageType = ChatMessageType.TEXT,
            createdAtMs = timestampMs + 1,
            status = LocalMessageStatus.SENDING,
            isPlaceholder = false,
            draftForRetry = null,
            streamingBuffer = "",
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
                        assistantLocalId -> {
                            null
                        }

                        userLocalId -> {
                            msg.copy(
                                status = LocalMessageStatus.FAILED,
                                errorMessage = error.message,
                            )
                        }

                        else -> {
                            msg
                        }
                    }
                }
            state.copy(pending = updatedPending)
        }
    }

    private suspend fun sendMessageLegacy(
        conversationId: String,
        draft: SendMessageDraft,
        userLocalId: String,
        assistantLocalId: String,
        sentAtMs: Long,
    ) {
        sendMessageUseCase(
            SendMessageUseCase.Params(
                conversationId = conversationId,
                draft = draft,
            ),
        ).onSuccess { result ->
            handleSendSuccess(result, userLocalId, assistantLocalId, sentAtMs)
        }.onFailure { error ->
            handleSendFailure(error, userLocalId, assistantLocalId)
        }
    }

    private fun sendHumanChatMessage(
        conversationId: String,
        draft: SendMessageDraft,
        userLocal: LocalMessage,
        userLocalId: String,
        assistantLocalId: String,
        sentAtMs: Long,
    ) {
        _overlay.update { it.copy(pending = it.pending + userLocal) }
        viewModelScope.launch {
            sendHumanMessageUseCase(
                SendHumanMessageUseCase.Params(
                    conversationId = conversationId,
                    draft = draft,
                ),
            ).onSuccess { result ->
                handleSendSuccess(result, userLocalId, assistantLocalId, sentAtMs)
            }.onFailure { error ->
                handleSendFailure(error, userLocalId, assistantLocalId)
            }
        }
    }

    private fun sendLegacyMessageWithPlaceholder(
        conversationId: String,
        draft: SendMessageDraft,
        userLocal: LocalMessage,
        userLocalId: String,
        assistantLocalId: String,
        timestampMs: Long,
    ) {
        val assistantPlaceholder = createAssistantPlaceholder(timestampMs)
        _overlay.update { it.copy(pending = it.pending + userLocal + assistantPlaceholder) }
        activeReplyCount.update { it + 1 }
        viewModelScope.launch {
            try {
                sendMessageLegacy(
                    conversationId = conversationId,
                    draft = draft,
                    userLocalId = userLocalId,
                    assistantLocalId = assistantLocalId,
                    sentAtMs = timestampMs,
                )
            } finally {
                activeReplyCount.update { it - 1 }
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    fun sendMessage(draft: SendMessageDraft) {
        val convId = conversationId ?: return

        _assistantError.value = null

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
                collageId = draft.collageId,
                collageBotId = draft.collageBotId,
                collageDate = draft.collageDate,
            )

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

        if (_viewState.value.isHumanChat) {
            sendHumanChatMessage(
                conversationId = convId,
                draft = draft,
                userLocal = userLocal,
                userLocalId = localUserId,
                assistantLocalId = localAssistantId,
                sentAtMs = now,
            )
        } else if (shouldStream(draft)) {
            // Phase 7 (revised): when there is NO active stream we add the user
            // Local AND the streaming placeholder in a single _overlay.update so
            // Compose never composes an intermediate state where only the user
            // bubble exists. That intermediate state was producing a one-frame
            // "big pink box" — the LazyColumn measure pass briefly let the
            // unconstrained user bubble take its max width before settling to
            // content-shaped width on the next pass (Soma repro, sporadic).
            //
            // When a stream IS in flight we still enqueue and let
            // drainStreamingQueue add the placeholder later — the user's bubble
            // appears immediately, and the assistant placeholder lands when the
            // prior stream terminates. That sustained "user bubble visible
            // without streaming below it" state is a queued send, not a
            // measure-pass flash, so it doesn't reproduce the regression.
            if (activeStreamJob?.isActive == true) {
                _overlay.update { it.copy(pending = it.pending + userLocal) }
                pendingStreamingQueue.addLast(
                    QueuedStreamingSend(
                        draft = draft,
                        userLocalId = localUserId,
                        timestampMs = now,
                    ),
                )
            } else {
                val streamingPlaceholder = createStreamingAssistantPlaceholder(now)
                _overlay.update {
                    it.copy(pending = it.pending + userLocal + streamingPlaceholder)
                }
                activeStreamJob =
                    startStreamingAssistantReply(
                        conversationId = convId,
                        draft = draft,
                        streamingLocalId = streamingPlaceholder.localId,
                        userLocalId = localUserId,
                    )
            }
        } else {
            sendLegacyMessageWithPlaceholder(
                conversationId = convId,
                draft = draft,
                userLocal = userLocal,
                userLocalId = localUserId,
                assistantLocalId = localAssistantId,
                timestampMs = now,
            )
        }
    }

    /**
     * Phase 6: re-run the last failed stream using the cached retry draft.
     * The user's preceding USER Local is still in `_overlay.pending` (the
     * Failed handler only dropped the streaming placeholder), so we don't
     * create a duplicate user bubble — we just append a fresh streaming
     * placeholder and restart the SSE collect. The error bubble clears on
     * the first token of the new stream (Phase 6 Token handler).
     *
     * No-ops when there is no retryable error queued, when the carry-over
     * draft is absent, or when the active conversation id has changed.
     */
    @OptIn(ExperimentalTime::class)
    // Early-exit guard sequence — each `return` is a no-op precondition check
    // (no error, no draft, no conversation, no user). Refactoring into a
    // single condition would be less readable than the guard cascade.
    @Suppress("ReturnCount")
    fun retryFailedAssistantReply() {
        val presentation = _assistantError.value ?: return
        val draft = presentation.retryDraft ?: return
        conversationId ?: return
        val userLocalId =
            _overlay.value.pending
                .lastOrNull { it.role == ConversationMessageRole.USER }
                ?.localId
                ?: return

        // Phase 7: route the retry through the same single-stream queue. If
        // somehow another stream is mid-flight (e.g. user typed quickly), this
        // waits its turn instead of racing.
        val now = Clock.System.now().toEpochMilliseconds()
        pendingStreamingQueue.addLast(
            QueuedStreamingSend(
                draft = draft,
                userLocalId = userLocalId,
                timestampMs = now,
            ),
        )
        drainStreamingQueue()
    }

    /**
     * Phase 2.7 streaming routing decision. Conservative carve-outs:
     *  - Feature flag must be ON.
     *  - Text-only drafts (no media or audio attachments).
     *  - No active Chat-as-Human takeover (the backend's chat.py early-exit
     *    suppresses the AI reply during takeover).
     *  Phase 9 will add the consecutive-failure circuit breaker and the
     *  is_nsfw conversation carve-out once that field is exposed on the
     *  conversation DTO.
     */
    private fun shouldStream(draft: SendMessageDraft): Boolean {
        val s = _viewState.value
        return s.isSseStreamingEnabled &&
            draft.messageType == ChatMessageType.TEXT &&
            draft.mediaAttachments.isEmpty() &&
            draft.audioAttachment == null &&
            !s.isHumanCreatorTakeoverActive &&
            consecutiveStreamFailures < STREAM_FAILURE_CIRCUIT_BREAKER
    }

    /**
     * Phase 7: process the next queued streaming send if no stream is currently
     * in flight. No-ops while [activeStreamJob] is alive — the active stream's
     * `finally` block calls this again on completion so the queue drains FIFO.
     *
     * Drops queued items whose conversation is no longer the active one (e.g.
     * user navigated away) — partial replies are intentionally not promoted
     * across conversations.
     *
     * Each `return` is a precondition gate (already streaming → wait;
     * queue empty → no-op; conversation gone → drop). Combining them would
     * obscure the queue-state state-machine — hence the [Suppress] on
     * `ReturnCount`.
     */
    @Suppress("ReturnCount")
    private fun drainStreamingQueue() {
        if (activeStreamJob?.isActive == true) return
        val next = pendingStreamingQueue.removeFirstOrNull() ?: return
        val convId = conversationId
        if (convId == null) {
            // Conversation switched while items were queued. Discard the rest.
            pendingStreamingQueue.clear()
            return
        }
        val streamingPlaceholder = createStreamingAssistantPlaceholder(next.timestampMs)
        _overlay.update { it.copy(pending = it.pending + streamingPlaceholder) }
        activeStreamJob =
            startStreamingAssistantReply(
                conversationId = convId,
                draft = next.draft,
                streamingLocalId = streamingPlaceholder.localId,
                userLocalId = next.userLocalId,
            )
    }

    /** See the call site comment in startStreamingAssistantReply.Done for the rationale. */
    @OptIn(ExperimentalTime::class)
    private fun reconcileUserMessageAfterStream(
        conversationId: String,
        userLocalId: String,
    ) {
        viewModelScope.launch {
            runSuspendCatching {
                chatRepository.getConversationMessagesPage(
                    conversationId = conversationId,
                    limit = USER_RECONCILE_FETCH_LIMIT,
                    offset = 0,
                )
            }.onSuccess { result ->
                val userMessage =
                    result.messages.firstOrNull { it.role == ConversationMessageRole.USER }
                        ?: run {
                            Logger.w("SSE") { "reconcile: no USER message in latest page; dropping local-only" }
                            // Even if the server fetch missed it (weird race), drop the optimistic
                            // Local so it doesn't accumulate. Paging will hydrate the real one
                            // on the next refresh / re-entry.
                            _overlay.update { state ->
                                state.copy(pending = state.pending.filterNot { it.localId == userLocalId })
                            }
                            return@onSuccess
                        }
                val userInsertedAtMs = parseTimestampToEpochMs(userMessage.createdAt)
                Logger.i("SSE") {
                    "reconcile: claiming USER Local $userLocalId → server id=${userMessage.id} " +
                        "insertedAtMs=$userInsertedAtMs"
                }
                _overlay.update { state ->
                    val newPending = state.pending.filterNot { it.localId == userLocalId }
                    val newSent =
                        if (userInsertedAtMs != null) {
                            state.sent + SentMessage(insertedAtMs = userInsertedAtMs, message = userMessage)
                        } else {
                            state.sent
                        }
                    state.copy(pending = newPending, sent = newSent)
                }
            }.onFailure { error ->
                Logger.w(ConversationViewModel::class.simpleName!!, error) {
                    "reconcile fetch failed conv=$conversationId; leaving USER Local in place for next session"
                }
            }
        }
    }

    private fun freezeStreamingAssistantPartial(streamingLocalId: String) {
        _overlay.update { state ->
            state.copy(
                pending =
                    state.pending.map { msg ->
                        if (msg.localId == streamingLocalId) {
                            msg.copy(
                                content = msg.streamingBuffer,
                                streamingBuffer = null,
                            )
                        } else {
                            msg
                        }
                    },
            )
        }
    }

    @OptIn(ExperimentalTime::class)
    @Suppress(
        // SSE Token/Done/Failed branches + Phase 5c coalescer + Phase 7
        // watchdog + Phase 9 circuit breaker + retry/queue ordering are
        // inherently coupled — splitting into helpers would force shared
        // mutable state (lastActivityMs, idleTimedOut, pendingTokenText,
        // flushJob) through parameter lists and lose readability.
        "LongMethod",
        "CyclomaticComplexMethod",
    )
    private fun startStreamingAssistantReply(
        conversationId: String,
        draft: SendMessageDraft,
        streamingLocalId: String,
        userLocalId: String,
    ): Job {
        // Synchronous increment so the send button greys on the SAME frame the
        // stream is scheduled — no enabled-tap window between sendMessage's
        // return and the launch body actually starting to execute.
        activeReplyCount.update { it + 1 }
        return viewModelScope.launch {
            // Phase 7 idle watchdog. `lastActivityMs` is updated on every event
            // received from the SSE collect; the watchdog wakes after a calibrated
            // wait and trips if the gap since the last event has reached
            // SSE_IDLE_TIMEOUT_MS. Tripping sets `idleTimedOut = true` and cancels
            // this launch via the SSE_IDLE_CANCEL_MESSAGE sentinel, so the finally
            // block can distinguish watchdog cancellation from navigation/reset.
            var lastActivityMs = Clock.System.now().toEpochMilliseconds()
            var idleTimedOut = false
            val streamJob = coroutineContext[Job]
            val watchdogJob =
                launch {
                    while (isActive) {
                        val sinceLastEventMs = Clock.System.now().toEpochMilliseconds() - lastActivityMs
                        val waitMs = (SSE_IDLE_TIMEOUT_MS - sinceLastEventMs).coerceAtLeast(SSE_WATCHDOG_MIN_TICK_MS)
                        delay(waitMs)
                        if (Clock.System.now().toEpochMilliseconds() - lastActivityMs >= SSE_IDLE_TIMEOUT_MS) {
                            idleTimedOut = true
                            streamJob?.cancel(SSE_IDLE_CANCEL_MESSAGE)
                            return@launch
                        }
                    }
                }

            try {
                val pendingTokenText = StringBuilder()
                var flushJob: Job? = null
                var hasRenderedToken = false

                suspend fun applyPendingTokens() {
                    val text = pendingTokenText.toString()
                    pendingTokenText.clear()
                    if (text.isEmpty()) return
                    _overlay.update { state ->
                        state.copy(
                            pending =
                                state.pending.map { msg ->
                                    if (msg.localId == streamingLocalId) {
                                        val newBuffer = (msg.streamingBuffer.orEmpty()) + text
                                        val lock =
                                            msg.useMarkdownLocked ?: if (newBuffer.isNotEmpty()) {
                                                newBuffer.shouldRenderAsMarkdown()
                                            } else {
                                                null
                                            }
                                        hasRenderedToken = true
                                        msg.copy(
                                            streamingBuffer = newBuffer,
                                            useMarkdownLocked = lock,
                                        )
                                    } else {
                                        msg
                                    }
                                },
                        )
                    }
                }

                runSuspendCatching {
                    // Phase 5c: token coalescing. Tokens are appended to `pendingTokenText`
                    // and applied to `_overlay` once per ~250ms quiet window (or immediately
                    // on Done/Failed). This reduces the streaming Local's `streamingBuffer`
                    // updates to 1-3 per reply, which is the verified mitigation for the
                    // Markdown library's per-batch re-parse cost (Phase 5b probe confirmed
                    // re-parse is the root cause of per-batch flicker).
                    //
                    // Invariants:
                    //   - Single-flusher: a previous flushJob is cancelled before scheduling
                    //     a new one; Done/Failed cancelAndJoin to guarantee no in-flight
                    //     flush races the Local→Remote swap.
                    //   - applyPendingTokens is the ONLY writer that grows the streaming
                    //     buffer — the path lock is computed here on the first non-empty
                    //     batch and pinned for the rest of the stream.
                    chatRepository
                        .streamMessage(conversationId = conversationId, draft = draft)
                        .collect { event ->
                            // Phase 7: any event resets the idle watchdog. Done/Failed
                            // events still keep this updated so the watchdog doesn't
                            // race the terminal handler.
                            lastActivityMs = Clock.System.now().toEpochMilliseconds()
                            when (event) {
                                is StreamEvent.Token -> {
                                    Logger.i("SSE") { "token len=${event.text.length}" }
                                    // Phase 6: first token of a retry attempt clears any stale error
                                    // bubble so the UI doesn't show "Try again" next to a stream that
                                    // is now succeeding.
                                    if (_assistantError.value != null) {
                                        _assistantError.value = null
                                    }
                                    pendingTokenText.append(event.text)
                                    flushJob?.cancel()
                                    flushJob =
                                        launch {
                                            delay(TOKEN_COALESCE_WINDOW_MS)
                                            applyPendingTokens()
                                        }
                                }

                                is StreamEvent.Done -> {
                                    flushJob?.cancelAndJoin()
                                    applyPendingTokens()
                                    // Phase 9: a successful Done clears the circuit-breaker. The
                                    // session can keep using SSE for subsequent sends.
                                    consecutiveStreamFailures = 0
                                    Logger.i("SSE") {
                                        "done id=${event.assistantMessage.id} blocked=${event.blocked} " +
                                            "createdAt=${event.assistantMessage.createdAt}"
                                    }
                                    val msg = event.assistantMessage
                                    val insertedAtMs = parseTimestampToEpochMs(msg.createdAt)
                                    Logger.i("SSE") {
                                        "done parsedInsertedAtMs=$insertedAtMs (null means parse failed)"
                                    }
                                    // Phase 5b: persist the streaming Local's path lock onto the server
                                    // message id so the Remote replacement renders identically.
                                    // Default to `false` (Text) if the stream never produced a non-empty
                                    // chunk to evaluate against — e.g. error-only streams.
                                    val capturedLock =
                                        _overlay.value.pending
                                            .firstOrNull { it.localId == streamingLocalId }
                                            ?.useMarkdownLocked
                                            ?: false
                                    _streamMarkdownLockedRemoteIds.update { it + (msg.id to capturedLock) }
                                    Logger.i("SSE") { "done markdownLock for id=${msg.id} = $capturedLock" }
                                    _overlay.update { state ->
                                        val newPending = state.pending.filterNot { it.localId == streamingLocalId }
                                        val newSent =
                                            if (insertedAtMs != null) {
                                                state.sent + SentMessage(insertedAtMs = insertedAtMs, message = msg)
                                            } else {
                                                state.sent
                                            }
                                        Logger.i("SSE") {
                                            "done overlay update: pending ${state.pending.size}→${newPending.size} " +
                                                "sent ${state.sent.size}→${newSent.size} " +
                                                "loadedIds size=${loadedMessageIds.value.size} " +
                                                "msg.id in loadedIds=${msg.id in loadedMessageIds.value}"
                                        }
                                        state.copy(pending = newPending, sent = newSent)
                                    }
                                    // NOTE: do NOT pre-add msg.id to loadedMessageIds here. Let the
                                    // natural paging cycle add the id once paging hydrates the message;
                                    // dedup against the overlay then happens at the screen layer via
                                    // `isDuplicateOfOverlay`.

                                    // Reconcile the optimistic USER Local: the SSE `done` event only
                                    // carries the assistant message (per docs/SSE-PROTOCOL.md), so the
                                    // user's optimistic Local has no server-truth twin to swap with.
                                    // Without this fetch, the USER Local accumulates in `_overlay.pending`
                                    // across every send within a session — observed live in the Phase 3
                                    // diagnostic log (pending 2→1, then 3→2, then 6→5, etc.).
                                    // We fetch the latest 2 messages, find the user-role one, swap our
                                    // pending Local for a server-truth Remote, and let loadedMessageIds
                                    // dedup handle the overlay→paging handoff naturally.
                                    reconcileUserMessageAfterStream(
                                        conversationId = conversationId,
                                        userLocalId = userLocalId,
                                    )
                                }

                                is StreamEvent.Failed -> {
                                    // Phase 5c: cancel any pending coalesce flush before dropping
                                    // the streaming Local so we don't race with applyPendingTokens
                                    // re-adding text after we've removed the placeholder.
                                    flushJob?.cancelAndJoin()
                                    pendingTokenText.clear()
                                    // Phase 9: a backend-emitted Failed event counts toward the
                                    // circuit breaker, same as a connection-level failure.
                                    consecutiveStreamFailures += 1
                                    val assistantError = event.error
                                    Logger.w(ConversationViewModel::class.simpleName!!) {
                                        "Streaming failed: code=${assistantError.code} " +
                                            "rawCode=${assistantError.rawCode} " +
                                            "message=${assistantError.message} " +
                                            "retryable=${assistantError.retryable} conv=$conversationId"
                                    }
                                    if (hasRenderedToken) {
                                        freezeStreamingAssistantPartial(streamingLocalId)
                                    } else {
                                        _overlay.update { state ->
                                            state.copy(
                                                pending = state.pending.filterNot { it.localId == streamingLocalId },
                                            )
                                        }
                                    }
                                    // Phase 6: surface the error in the assistant slot. Retain the
                                    // draft alongside it so a retry tap restarts the stream without
                                    // having to re-derive the draft from the (still-present) USER
                                    // Local in overlay.
                                    _assistantError.value =
                                        AssistantErrorPresentation(
                                            error = assistantError,
                                            retryDraft = if (assistantError.retryable) draft else null,
                                        )
                                }
                            }
                        }
                }.onFailure { error ->
                    if (idleTimedOut) return@onFailure
                    Logger.w(ConversationViewModel::class.simpleName!!, error) {
                        "Streaming connection failed conv=$conversationId"
                    }
                    flushJob?.cancelAndJoin()
                    consecutiveStreamFailures += 1
                    if (hasRenderedToken) {
                        freezeStreamingAssistantPartial(streamingLocalId)
                        _assistantError.value =
                            AssistantErrorPresentation(
                                error =
                                    AssistantError(
                                        code = AssistantErrorCode.TRANSIENT,
                                        rawCode = "TRANSIENT",
                                        message = getString(Res.string.assistant_error_connection_timed_out),
                                        retryable = true,
                                    ),
                                retryDraft = draft,
                            )
                    } else {
                        pendingTokenText.clear()
                        // If the stream fails before anything is visible (404 during rollout,
                        // connection refused, TLS reset), complete the SAME optimistic send via
                        // the legacy endpoint. This is the "silent fallback" path promised by
                        // the handoff docs, and it prevents the USER Local from staying stuck
                        // in SENDING just because SSE is unavailable.
                        sendMessageLegacy(
                            conversationId = conversationId,
                            draft = draft,
                            userLocalId = userLocalId,
                            assistantLocalId = streamingLocalId,
                            sentAtMs = Clock.System.now().toEpochMilliseconds(),
                        )
                    }
                }
            } finally {
                watchdogJob.cancel()
                if (idleTimedOut && _assistantError.value == null) {
                    // Phase 7: synthesize a TRANSIENT AssistantError so the user gets
                    // the same retryable UI as a real backend error event. Only fires
                    // when no error was already set by the Failed handler.
                    handleSseIdleTimeoutAsTransient(
                        streamingLocalId = streamingLocalId,
                        draft = draft,
                        conversationId = conversationId,
                    )
                }
                if (activeStreamJob === coroutineContext[Job]) {
                    activeStreamJob = null
                }
                // Drain regardless of how the stream ended. If a queued send is
                // waiting, it starts now; otherwise no-op. drainStreamingQueue
                // may itself call startStreamingAssistantReply which increments
                // activeReplyCount before this stream's decrement runs — that
                // ordering keeps the count > 0 across the transition so the
                // send button doesn't flicker enabled between back-to-back
                // queued replies.
                drainStreamingQueue()
                activeReplyCount.update { it - 1 }
            }
        }
    }

    private suspend fun handleSseIdleTimeoutAsTransient(
        streamingLocalId: String,
        draft: SendMessageDraft,
        conversationId: String,
    ) {
        Logger.w(ConversationViewModel::class.simpleName!!) {
            "SSE stream idle for ${SSE_IDLE_TIMEOUT_MS}ms — synthesizing TRANSIENT " +
                "conv=$conversationId"
        }
        // Phase 9: an idle timeout is functionally a connection failure for the
        // circuit-breaker accounting.
        consecutiveStreamFailures += 1
        _overlay.update { state ->
            state.copy(pending = state.pending.filterNot { it.localId == streamingLocalId })
        }
        _assistantError.value =
            AssistantErrorPresentation(
                error =
                    AssistantError(
                        code = AssistantErrorCode.TRANSIENT,
                        rawCode = "TRANSIENT",
                        message = getString(Res.string.assistant_error_connection_timed_out),
                        retryable = true,
                    ),
                retryDraft = draft,
            )
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

    // ── Collage (Request Images) ─────────────────────────────────────────
    // Chat messages carry only a reference (collageId when present, plus
    // botId + date); bubbles resolve it here at render time so the URLs
    // always match the CURRENT subscription state (blurred for
    // non-subscribers, clear after purchase — including for historical
    // messages, which refetch on the flip).

    private val _collageStates = MutableStateFlow<Map<String, CollageUiState>>(emptyMap())
    val collageStates: StateFlow<Map<String, CollageUiState>> = _collageStates.asStateFlow()

    private val _requestImageCooldownSeconds = MutableStateFlow<Int?>(null)
    val requestImageCooldownSeconds: StateFlow<Int?> = _requestImageCooldownSeconds.asStateFlow()

    private val collageFetchJobs = mutableMapOf<String, Job>()

    // Everything needed to (re)fetch a collage reference. collageId is the
    // preferred server handle; legacy messages predate it and resolve via
    // (botId, date). Client-side identity stays (botId, date) — one collage
    // per bot per day — so both message shapes share one key space.
    private data class CollageRef(
        val botId: String,
        val date: String,
        val collageId: String?,
    ) {
        val key: String get() = "$botId|$date"
    }

    private val collageRefs = mutableMapOf<String, CollageRef>()
    private var collageCooldownJob: Job? = null
    private var collageHasChatAccess = false
    private var isCollageRequestInFlight = false

    /**
     * Pushed from the screen — the effective subscription gate (Pro OR
     * per-influencer access) is composed there, outside this VM. A flip
     * refetches every collage currently tracked with the new is_subscribed
     * so blurred and clear bubbles never coexist in one conversation.
     */
    fun setHasChatAccess(hasAccess: Boolean) {
        if (collageHasChatAccess == hasAccess) return
        collageHasChatAccess = hasAccess
        if (collageRefs.isEmpty()) return
        Logger.d("CollageX") { "hasChatAccess -> $hasAccess, refetching ${collageRefs.size} collage(s)" }
        collageFetchJobs.values.forEach { it.cancel() }
        collageFetchJobs.clear()
        _collageStates.update { state -> state.mapValues { CollageUiState.Loading } }
        collageRefs.values.toList().forEach { launchCollageFetch(it) }
    }

    /** Render-time resolve of a collage reference; called from the bubble's LaunchedEffect. */
    @OptIn(ExperimentalTime::class)
    @Suppress("ReturnCount") // precondition gates: blank ref, already resolved, in flight
    fun loadCollage(
        botId: String,
        date: String,
        collageId: String?,
    ) {
        if (botId.isBlank() || date.isBlank()) return
        // Cooldown sighting: a collage dated today in this conversation means
        // today's request is already spent — the conversation history is the
        // server-persisted record, so this survives reinstalls and devices.
        if (date == currentCollageDateString()) {
            startCollageCooldown()
        }
        val ref = CollageRef(botId = botId, date = date, collageId = collageId)
        // Keep the richest ref seen for this key: an id-less sighting (e.g.
        // the optimistic Local echo racing the Remote row) must not erase a
        // collageId already recorded for it.
        val tracked = collageRefs[ref.key]
        if (tracked == null || (tracked.collageId == null && collageId != null)) {
            collageRefs[ref.key] = ref
        }
        if (_collageStates.value[ref.key] is CollageUiState.Ready) return
        if (collageFetchJobs[ref.key]?.isActive == true) return
        val cached = collageCache.read(botId = botId, date = date, isSubscribed = collageHasChatAccess)
        if (cached != null) {
            _collageStates.update { it + (ref.key to CollageUiState.Ready(cached)) }
            return
        }
        _collageStates.update { it + (ref.key to CollageUiState.Loading) }
        launchCollageFetch(collageRefs.getValue(ref.key))
    }

    @OptIn(ExperimentalTime::class)
    private fun launchCollageFetch(ref: CollageRef) {
        val key = ref.key
        collageFetchJobs[key] =
            viewModelScope.launch {
                val isSubscribed = collageHasChatAccess
                var attempt = 0
                var terminal = false
                while (!terminal) {
                    var retryAfterMs: Long? = null
                    Logger.d("CollageX") {
                        "fetch GET bot=${ref.botId} date=${ref.date} collageId=${ref.collageId} " +
                            "isSubscribed=$isSubscribed attempt=$attempt"
                    }
                    getInfluencerCollageUseCase(
                        GetInfluencerCollageUseCase.Params(
                            influencerId = ref.botId,
                            isSubscribed = isSubscribed,
                            collageId = ref.collageId,
                            date = ref.date,
                        ),
                    ).onSuccess { collage ->
                        terminal = true
                        Logger.d("CollageX") {
                            "fetch success key=$key collageId=${collage.id} date=${collage.date} " +
                                "images=${collage.images.size} isBlurred=${collage.isBlurred}"
                        }
                        collageCache.write(collage = collage, isSubscribed = isSubscribed)
                        _collageStates.update { it + (key to CollageUiState.Ready(collage)) }
                    }.onFailure { error ->
                        // 404 on an id-less reference dated today = collage
                        // still generating (cold-path race, e.g. another device
                        // just POSTed) — retry briefly. A reference WITH an id
                        // means the collage already exists, so its 404 (like
                        // any other error) degrades to Unavailable; a
                        // scroll-back re-entry retriggers loadCollage.
                        val stillGenerating =
                            error.httpStatusOrNull() == HttpStatusCode.NotFound &&
                                ref.collageId == null &&
                                ref.date == currentCollageDateString()
                        Logger.e("CollageX", error) {
                            "fetch failed key=$key status=${error.httpStatusOrNull()} " +
                                "type=${error::class.simpleName} msg=${error.message} " +
                                "stillGenerating=$stillGenerating attempt=$attempt"
                        }
                        if (stillGenerating && attempt < COLLAGE_NOT_READY_RETRY_DELAYS_MS.size) {
                            retryAfterMs = COLLAGE_NOT_READY_RETRY_DELAYS_MS[attempt]
                        } else {
                            terminal = true
                            _collageStates.update { it + (key to CollageUiState.Unavailable) }
                        }
                    }
                    retryAfterMs?.let {
                        attempt++
                        delay(it)
                    }
                }
            }
    }

    /**
     * "Request Image" tap: POST request-images (consumes today's quota; the
     * rare cold path takes 45–65 s — shimmer placeholder meanwhile), then
     * send the (botId, date) reference into the conversation as a collage
     * message. The message never carries image URLs.
     */
    @OptIn(ExperimentalTime::class)
    @Suppress("ReturnCount") // precondition gates: no conversation, no influencer, H2H, cooldown
    fun requestCollageImages() {
        val convId = conversationId ?: return
        val influencer = _viewState.value.influencer ?: return
        if (_viewState.value.isHumanChat) return
        // Photo collages are exclusive to subscription bots (Tara).
        if (!BotSubscriptionCatalog.usesBotSubscription(influencer.id)) return
        if (isCollageRequestInFlight || _requestImageCooldownSeconds.value != null) return
        isCollageRequestInFlight = true
        Logger.d("CollageX") { "requestImages start bot=${influencer.id} isSubscribed=$collageHasChatAccess" }
        val now = Clock.System.now().toEpochMilliseconds()
        val shimmer =
            LocalMessage(
                localId = "local-collage-shimmer-$now",
                role = ConversationMessageRole.ASSISTANT,
                content = null,
                messageType = ChatMessageType.COLLAGE,
                createdAtMs = now,
                status = LocalMessageStatus.WAITING,
                isPlaceholder = true,
                draftForRetry = null,
            )
        _overlay.update { it.copy(pending = it.pending + shimmer) }
        viewModelScope.launch {
            try {
                requestInfluencerImagesUseCase(
                    RequestInfluencerImagesUseCase.Params(
                        influencerId = influencer.id,
                        isSubscribed = collageHasChatAccess,
                    ),
                ).onSuccess { collage ->
                    onCollageRequestSucceeded(convId, shimmer.localId, collage)
                }.onFailure { error ->
                    onCollageRequestFailed(shimmer.localId, error)
                }
            } finally {
                isCollageRequestInFlight = false
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun onCollageRequestSucceeded(
        convId: String,
        shimmerLocalId: String,
        collage: Collage,
    ) {
        Logger.d("CollageX") {
            "requestImages success collageId=${collage.id} date=${collage.date} " +
                "images=${collage.images.size} isBlurred=${collage.isBlurred} theme=${collage.theme}"
        }
        collageCache.write(collage = collage, isSubscribed = collageHasChatAccess)
        // Pre-warm so the bubble renders instantly once the message lands.
        val ref = CollageRef(botId = collage.botId, date = collage.date, collageId = collage.id)
        collageRefs[ref.key] = ref
        _collageStates.update { it + (ref.key to CollageUiState.Ready(collage)) }
        startCollageCooldown()
        val draft =
            SendMessageDraft(
                messageType = ChatMessageType.COLLAGE,
                // Fallback body: old clients degrade unknown types to TEXT
                // and render this string instead of the grid.
                content = getString(Res.string.request_image_message),
                collageId = collage.id,
                collageBotId = collage.botId,
                collageDate = collage.date,
            )
        val now = Clock.System.now().toEpochMilliseconds()
        val userLocal =
            LocalMessage(
                localId = "local-user-$now",
                role = ConversationMessageRole.USER,
                content = draft.content,
                messageType = ChatMessageType.COLLAGE,
                createdAtMs = now,
                status = LocalMessageStatus.SENDING,
                isPlaceholder = false,
                draftForRetry = draft,
                collageId = collage.id,
                collageBotId = collage.botId,
                collageDate = collage.date,
            )
        _overlay.update { state ->
            state.copy(pending = state.pending.filterNot { it.localId == shimmerLocalId } + userLocal)
        }
        // Direct send — the generic sendMessage() pipeline would add an
        // assistant waiting placeholder + AI-reply telemetry, both wrong for
        // a collage share. Send failure marks the Local FAILED with
        // draftForRetry, so the existing tap-to-resend re-sends the reference
        // only (the collage itself is already generated; no second POST).
        Logger.d("CollageX") {
            "sending collage message convId=$convId collageId=${collage.id} " +
                "botId=${collage.botId} date=${collage.date}"
        }
        sendMessageUseCase(
            SendMessageUseCase.Params(conversationId = convId, draft = draft),
        ).onSuccess { result ->
            handleCollageSendSuccess(result, userLocal.localId, collage)
        }.onFailure { error ->
            Logger.e("CollageX", error) {
                "collage message send failed status=${error.httpStatusOrNull()} msg=${error.message}"
            }
            handleSendFailure(error, userLocal.localId, assistantLocalId = "")
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun handleCollageSendSuccess(
        result: SendMessageResult,
        userLocalId: String,
        collage: Collage,
    ) {
        Logger.d("CollageX") {
            val echo = result.userMessage
            "send echo id=${echo.id} type=${echo.messageType} collageId=${echo.collageId} " +
                "botId=${echo.collageBotId} date=${echo.collageDate} " +
                "hasContent=${!echo.content.isNullOrBlank()} createdAt=${echo.createdAt}"
        }
        // Self-heal the echo: the optimistic Local we're about to remove
        // rendered the grid from its collage refs. If the send endpoint
        // doesn't echo those refs (or the type) back, the swapped-in Remote
        // would render as an empty text bubble — re-attach what we just sent.
        // History rendering after a relaunch still needs the backend to
        // persist the refs; the log above is the evidence either way.
        val userMessage =
            result.userMessage.let { echo ->
                if (echo.collageBotId == null || echo.collageDate == null) {
                    echo.copy(
                        messageType = ChatMessageType.COLLAGE,
                        collageId = collage.id,
                        collageBotId = collage.botId,
                        collageDate = collage.date,
                    )
                } else {
                    echo
                }
            }
        // buildSentMessages drops entries whose created_at fails to parse —
        // for the collage echo, fall back to "now" instead of vanishing.
        val userInsertedAtMs =
            parseTimestampToEpochMs(userMessage.createdAt)
                ?: Clock.System.now().toEpochMilliseconds()
        val sentMessages =
            buildList {
                add(SentMessage(insertedAtMs = userInsertedAtMs, message = userMessage))
                result.assistantMessage?.let { assistant ->
                    parseTimestampToEpochMs(assistant.createdAt)?.let {
                        add(SentMessage(insertedAtMs = it, message = assistant))
                    }
                }
            }
        _overlay.update { state ->
            state.copy(
                pending = state.pending.filterNot { it.localId == userLocalId },
                sent = state.sent + sentMessages,
            )
        }
    }

    private suspend fun onCollageRequestFailed(
        shimmerLocalId: String,
        error: Throwable,
    ) {
        Logger.e("CollageX", error) {
            "requestImages failed status=${error.httpStatusOrNull()} type=${error::class.simpleName} " +
                "msg=${error.message} cause=${error.cause?.let { "${it::class.simpleName}: ${it.message}" }}"
        }
        _overlay.update { state ->
            state.copy(pending = state.pending.filterNot { it.localId == shimmerLocalId })
        }
        if (error.httpStatusOrNull() == HttpStatusCode.TooManyRequests) {
            // Server says today's quota is already spent (e.g. requested from
            // another device) — server stays authoritative over the local menu.
            startCollageCooldown()
            influencerSubscriptionToastChannel.trySend(
                InfluencerSubscriptionToastEvent(
                    ToastStatus.Info,
                    getString(Res.string.collage_quota_used_toast),
                ),
            )
        } else {
            influencerSubscriptionToastChannel.trySend(
                InfluencerSubscriptionToastEvent(
                    ToastStatus.Error,
                    getString(Res.string.collage_request_failed_toast),
                ),
            )
        }
    }

    /** Idempotent: starts the disabled-until-next-UTC-midnight countdown once. */
    @OptIn(ExperimentalTime::class)
    private fun startCollageCooldown() {
        if (collageCooldownJob?.isActive == true) return
        collageCooldownJob =
            viewModelScope.launch {
                while (isActive) {
                    val nowMs = Clock.System.now().toEpochMilliseconds()
                    val remainingSeconds = ((nextCollageResetMs(nowMs) - nowMs) / MS_PER_SECOND).toInt()
                    if (remainingSeconds <= 0) break
                    _requestImageCooldownSeconds.value = remainingSeconds
                    delay(1.seconds)
                }
                _requestImageCooldownSeconds.value = null
            }
    }

    private fun resetCollageState() {
        collageFetchJobs.values.forEach { it.cancel() }
        collageFetchJobs.clear()
        collageRefs.clear()
        _collageStates.value = emptyMap()
        collageCooldownJob?.cancel()
        collageCooldownJob = null
        _requestImageCooldownSeconds.value = null
        isCollageRequestInFlight = false
    }

    // Collage-day arithmetic. The server quota window is the UTC calendar
    // day — its 429 says resets_at=T00:00:00+00:00 — and GET /collage
    // defaults to "today's UTC calendar date". (The 04:00 UTC nightly
    // pre-gen is only when generation runs, not the quota boundary.)
    private fun currentCollageDayIndex(nowMs: Long): Long = nowMs.floorDiv(MS_PER_DAY)

    private fun nextCollageResetMs(nowMs: Long): Long = (currentCollageDayIndex(nowMs) + 1) * MS_PER_DAY

    /** Today's collage_date ("YYYY-MM-DD"), for comparing against message references. */
    @OptIn(ExperimentalTime::class)
    private fun currentCollageDateString(): String =
        LocalDate
            .fromEpochDays(currentCollageDayIndex(Clock.System.now().toEpochMilliseconds()).toInt())
            .toString()

    private data class OverlayState(
        val pending: List<LocalMessage> = emptyList(),
        val sent: List<SentMessage> = emptyList(),
    )

    private data class SentMessage(
        val insertedAtMs: Long,
        val message: ChatMessage,
    )

    fun clearError() {
        _viewState.update { it.copy(chatError = null) }
    }

    fun retryConversationCreation() {
        val influencerId = _viewState.value.influencer?.id ?: return
        clearError()
        createConversation(influencerId)
    }

    fun retryDeleteAndRecreateConversation() {
        val influencerId = _viewState.value.influencer?.id ?: return
        clearError()
        deleteAndRecreateConversation(influencerId)
    }

    fun markConversationAsRead(conversationId: String) {
        if (_viewState.value.isBotAccount) return
        viewModelScope.launch {
            markConversationAsReadUseCase(conversationId)
                .onSuccess { chatUnreadRefreshSignal.requestRefresh() }
                .onFailure { error ->
                    Logger.w(ConversationViewModel::class.simpleName!!, error) {
                        "Failed to mark conversation as read: $conversationId"
                    }
                }
        }
    }

    // ── Human Creator Takeover (Chat as Human) ──────────────────────────────
    // Polling-based. No WebSocket dependency. See PLAN-CHAT-AS-HUMAN.md.
    private var takeoverPollingJob: Job? = null
    private var takeoverCountdownJob: Job? = null

    fun startHumanCreatorTakeover() {
        val state = _viewState.value
        val convId = state.conversationId
        val featureUnavailable = !state.isChatAsHumanCreatorEnabled || !state.isBotAccount
        val takeoverUnavailable = state.isHumanCreatorTakeoverActive || state.isHumanCreatorTakeoverStarting
        if (featureUnavailable || convId == null || takeoverUnavailable) {
            return
        }
        _viewState.update { it.copy(isHumanCreatorTakeoverStarting = true) }
        viewModelScope.launch {
            startHumanCreatorTakeoverUseCase(convId)
                .onSuccess { status ->
                    applyTakeoverStatus(status.active, status.remainingSeconds)
                    _viewState.update { it.copy(isHumanCreatorTakeoverStarting = false) }
                    if (status.active) {
                        // Pull the "X has joined the chat" banner into the overlay
                        // immediately. Otherwise it doesn't show until the first poll
                        // tick ~3s later, and the creator can start typing before the
                        // banner appears — confusing UX.
                        pollLatestMessagesIntoOverlay(convId)
                        startTakeoverPolling(convId)
                    }
                }.onFailure { error ->
                    Logger.w(ConversationViewModel::class.simpleName!!, error) {
                        "Failed to start human creator takeover: $convId"
                    }
                    _viewState.update { it.copy(isHumanCreatorTakeoverStarting = false) }
                }
        }
    }

    fun releaseHumanCreatorTakeover() {
        val convId = _viewState.value.conversationId ?: return
        if (!_viewState.value.isHumanCreatorTakeoverActive) return
        _viewState.update { it.copy(isHumanCreatorTakeoverEnding = true) }
        stopTakeoverPolling()
        viewModelScope.launch {
            releaseHumanCreatorTakeoverUseCase(convId)
                .onSuccess {
                    applyTakeoverStatus(active = false, remainingSeconds = 0)
                    _viewState.update { it.copy(isHumanCreatorTakeoverEnding = false) }
                    // Pull the "X has left the chat" system banner into the overlay
                    // without flickering the whole LazyColumn.
                    pollLatestMessagesIntoOverlay(convId)
                }.onFailure { error ->
                    Logger.w(ConversationViewModel::class.simpleName!!, error) {
                        "Failed to release human creator takeover: $convId"
                    }
                    // Local intent is to release — flip OFF anyway. Server will auto-release after 2 min.
                    applyTakeoverStatus(active = false, remainingSeconds = 0)
                    _viewState.update { it.copy(isHumanCreatorTakeoverEnding = false) }
                }
        }
    }

    @OptIn(ExperimentalTime::class)
    fun sendAsHumanCreator(content: String) {
        val state = _viewState.value
        val convId = state.conversationId
        val trimmed = content.trim()
        val messageUnavailable = convId == null || trimmed.isEmpty()
        val takeoverUnavailable = !state.isHumanCreatorTakeoverActive || state.isHumanCreatorMessageSending
        if (messageUnavailable || takeoverUnavailable) {
            return
        }
        _viewState.update { it.copy(isHumanCreatorMessageSending = true) }
        viewModelScope.launch {
            sendHumanCreatorMessageUseCase(
                SendHumanCreatorMessageUseCase.Params(conversationId = convId, content = trimmed),
            ).onSuccess { message ->
                // No optimistic UI here. Optimistic pending → sent caused a Local→Remote
                // swap that compose's items() rendered as a subtree teardown+rebuild
                // (flicker), and gave the auto-scroll a second target to animate to
                // (more flicker). Just add the real message to the overlay after the
                // POST returns. ~200-500ms latency, button greys during the wait.
                val insertedAtMs =
                    parseTimestampToEpochMs(message.createdAt)
                        ?: Clock.System.now().toEpochMilliseconds()
                _overlay.update { state ->
                    state.copy(sent = state.sent + SentMessage(insertedAtMs = insertedAtMs, message = message))
                }
                _viewState.update { it.copy(isHumanCreatorMessageSending = false) }
            }.onFailure { error ->
                Logger.w(ConversationViewModel::class.simpleName!!, error) {
                    "Failed to send human creator message: $convId"
                }
                _viewState.update { it.copy(isHumanCreatorMessageSending = false) }
            }
        }
    }

    fun refreshHumanCreatorTakeoverStatus() {
        val state = _viewState.value
        val convId = state.conversationId
        if (!state.isChatAsHumanCreatorEnabled || !state.isBotAccount || convId == null) return
        viewModelScope.launch {
            getHumanCreatorTakeoverStatusUseCase(convId)
                .onSuccess { status ->
                    applyTakeoverStatus(status.active, status.remainingSeconds)
                    if (status.active && takeoverPollingJob?.isActive != true) {
                        startTakeoverPolling(convId)
                    } else if (!status.active) {
                        stopTakeoverPolling()
                    }
                }.onFailure { error ->
                    Logger.w(ConversationViewModel::class.simpleName!!, error) {
                        "Failed to fetch takeover status: $convId"
                    }
                }
        }
    }

    private fun applyTakeoverStatus(
        active: Boolean,
        remainingSeconds: Int,
    ) {
        _viewState.update {
            it.copy(
                isHumanCreatorTakeoverActive = active,
                humanCreatorTakeoverRemainingSeconds = if (active) remainingSeconds.coerceAtLeast(0) else 0,
            )
        }
        if (active) startCountdownTicker() else stopCountdownTicker()
    }

    private fun startTakeoverPolling(conversationId: String) {
        stopTakeoverPolling()
        takeoverPollingJob =
            viewModelScope.launch {
                var iter = 0
                while (true) {
                    delay(TAKEOVER_MESSAGES_POLL_INTERVAL)
                    iter += 1
                    // Every 3s: fetch latest messages and merge new ones into the
                    // overlay. DO NOT call refreshHistory() — that resets the paging
                    // source, wipes loadedMessageIds, and rebuilds the entire LazyColumn
                    // every tick. That's what caused the flickering chat screen.
                    if (!_viewState.value.isHumanCreatorMessageSending) {
                        pollLatestMessagesIntoOverlay(conversationId)
                    }
                    // Every 5s (~every other iteration of 3s, but use modulo on time):
                    if (iter % TAKEOVER_STATUS_POLL_EVERY_N == 0) {
                        getHumanCreatorTakeoverStatusUseCase(conversationId)
                            .onSuccess { status ->
                                applyTakeoverStatus(status.active, status.remainingSeconds)
                                if (!status.active) return@launch
                            }
                    }
                }
            }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun pollLatestMessagesIntoOverlay(conversationId: String) {
        runSuspendCatching {
            chatRepository.getCreatorConversationMessagesPage(
                conversationId = conversationId,
                limit = TAKEOVER_POLL_PAGE_SIZE,
                offset = 0,
            )
        }.onSuccess { result ->
            val loadedIds = loadedMessageIds.value
            val sentIds =
                _overlay.value.sent
                    .map { it.message.id }
                    .toSet()
            val newSentMessages =
                result.messages
                    .filterNot { msg -> msg.id in loadedIds || msg.id in sentIds }
                    .mapNotNull { msg ->
                        val insertedAtMs = parseTimestampToEpochMs(msg.createdAt) ?: return@mapNotNull null
                        SentMessage(insertedAtMs = insertedAtMs, message = msg)
                    }
            if (newSentMessages.isNotEmpty()) {
                _overlay.update { it.copy(sent = it.sent + newSentMessages) }
            }
        }.onFailure { error ->
            Logger.w(ConversationViewModel::class.simpleName!!, error) {
                "pollLatestMessagesIntoOverlay failed: $conversationId"
            }
        }
    }

    private fun stopTakeoverPolling() {
        takeoverPollingJob?.cancel()
        takeoverPollingJob = null
    }

    private fun startCountdownTicker() {
        if (takeoverCountdownJob?.isActive == true) return
        takeoverCountdownJob =
            viewModelScope.launch {
                while (true) {
                    delay(1.seconds)
                    val current = _viewState.value
                    if (!current.isHumanCreatorTakeoverActive) return@launch
                    val next = (current.humanCreatorTakeoverRemainingSeconds - 1).coerceAtLeast(0)
                    _viewState.update { it.copy(humanCreatorTakeoverRemainingSeconds = next) }
                    if (next == 0) {
                        // Local countdown expired. Don't wait for the server-side sweep —
                        // proactively release so the UI flips off instantly AND the backend
                        // writes the "left" system message before any other path can.
                        releaseHumanCreatorTakeover()
                        return@launch
                    }
                }
            }
    }

    private fun stopCountdownTicker() {
        takeoverCountdownJob?.cancel()
        takeoverCountdownJob = null
    }

    override fun onCleared() {
        stopTakeoverPolling()
        stopCountdownTicker()
        super.onCleared()
    }
    // ────────────────────────────────────────────────────────────────────────

    private fun consumePurchaseInBackground(purchaseToken: String) {
        viewModelScope.launch {
            iapManager
                .consumePurchase(purchaseToken)
                .onFailure { Logger.e("SubscriptionX", it) { "Failed to consume purchase" } }
        }
    }

    /**
     * A restored purchase of [product] the billing service may not know
     * about yet: an unconsumed daily_chat from a failed grant, or an active
     * bot subscription missing its verify call. Retrying the grant instead
     * of re-purchasing is safe — both backend flows are idempotent.
     */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun findUngrantedChatPurchase(product: ProductId): Purchase? =
        try {
            iapManager
                .restorePurchases()
                .getOrNull()
                ?.purchases
                ?.firstOrNull { purchase ->
                    purchase.productId == product && purchase.state == PurchaseState.PURCHASED
                }
        } catch (_: Exception) {
            null
        }

    companion object {
        private const val PAGE_SIZE = 10
        private const val PREFETCH_DISTANCE = 5
        private const val MESSAGES_STOP_TIMEOUT_MS = 5_000L
        private const val FALLBACK_ACCESS_DURATION_MS = 24L * 60 * 60 * 1000
        private const val USER_RECONCILE_FETCH_LIMIT = 2
        private const val CACHE_WRITE_DEBOUNCE_MS = 500L
        private const val TOKEN_COALESCE_WINDOW_MS = 250L

        // Phase 7 idle watchdog. 30s of no token/done/error events on a live SSE
        // collect → treat as TRANSIENT failure (per SSE-IMPLEMENTATION-PLAN §2.3:
        // "if the stream closes without `done` or `error`, treat as `TRANSIENT`").
        private const val SSE_IDLE_TIMEOUT_MS = 30_000L

        // Sentinel cancellation message — the parent launch in
        // startStreamingAssistantReply distinguishes "watchdog tripped" from
        // structured-concurrency cancellation (navigation, resetState) by reading
        // this off the CancellationException, so navigation-away does NOT raise
        // a spurious error bubble.
        private const val SSE_IDLE_CANCEL_MESSAGE = "SSE idle timeout"

        // Watchdog wakes at this minimum cadence even when the previous tick's
        // computed sleep would be 0 — prevents a hot-spin in the pathological
        // case where the deadline has already passed but the parent hasn't
        // been cancelled yet.
        private const val SSE_WATCHDOG_MIN_TICK_MS = 500L

        // Phase 9 circuit breaker threshold. Three consecutive SSE failures
        // (Failed event / idle timeout / connection error) silently force the
        // rest of the session onto the non-streaming legacy endpoint. Resets to
        // 0 on any successful Done.
        private const val STREAM_FAILURE_CIRCUIT_BREAKER = 3
        private val TAKEOVER_MESSAGES_POLL_INTERVAL = 3.seconds

        // Status poll runs every Nth iteration of message poll: 3s × 2 ≈ 6s ≈ ~5s spec.
        private const val TAKEOVER_STATUS_POLL_EVERY_N = 2
        private const val TAKEOVER_POLL_PAGE_SIZE = 20

        private const val MS_PER_DAY = 24L * 60 * 60 * 1000
        private const val MS_PER_SECOND = 1000L

        // GET /collage 404s while today's collage is still generating
        // (cold-path race with another device's POST) — brief backoff.
        private val COLLAGE_NOT_READY_RETRY_DELAYS_MS = listOf(5_000L, 15_000L)
    }
}

data class InfluencerSubscriptionToastEvent(
    val status: ToastStatus,
    val message: String,
)

/** Render state of one collage reference, keyed by "botId|date" in [ConversationViewModel.collageStates]. */
sealed class CollageUiState {
    data object Loading : CollageUiState()

    data class Ready(
        val collage: Collage,
    ) : CollageUiState()

    /** Fetch failed terminally — older-than-today reference or exhausted retries. */
    data object Unavailable : CollageUiState()
}

data class ConversationViewState(
    val isCreating: Boolean = false,
    val isDeleting: Boolean = false,
    val chatError: ChatError? = null,
    val conversationId: String? = null,
    val influencer: ConversationInfluencer? = null,
    val isBotAccount: Boolean = false,
    val paginatedHistoryAvailable: Boolean = false,
    val shareDisplayName: String = "",
    val shareMessage: String = "",
    val shareDescription: String = "",
    val isSocialSignedIn: Boolean = false,
    val influencerSource: ConversationInfluencerSource = ConversationInfluencerSource.CARD,
    val loginPromptMessageThreshold: Int,
    val requireAuthBeforeFirstSend: Boolean = false,
    val subscriptionMandatoryThreshold: Int,
    val isSubscriptionEnabled: Boolean,
    val isInfluencerSubscriptionPurchasedAndVerified: Boolean = false,
    val isInfluencerSubscriptionAvailableToPurchase: Boolean = false,
    val isInfluencerSubscriptionPurchaseInProgress: Boolean = false,
    val influencerSubscriptionFormattedPrice: String? = null,
    val totalHistoryMessageCount: Int = 0,
    val chatAccessExpiresAtMs: Long? = null,
    val isChatAccessLoading: Boolean = false,
    val isChatAsHumanCreatorEnabled: Boolean = false,
    val isSseStreamingEnabled: Boolean = false,
    val isAudioRecordingEnabled: Boolean = false,
    val isHumanCreatorTakeoverActive: Boolean = false,
    val isHumanCreatorTakeoverStarting: Boolean = false,
    val isHumanCreatorTakeoverEnding: Boolean = false,
    val isHumanCreatorMessageSending: Boolean = false,
    val humanCreatorTakeoverRemainingSeconds: Int = 0,
    // H2H: present when this conversation is human-to-human (set by
    // initializeFromInbox when the navigation params carry the other
    // user's principal_id). [isHumanChat] is the derived single-source-of-
    // truth gate that all H2H-vs-AI branches in this module read from.
    val participantPrincipalId: String? = null,
    // H2H: viewer's own principal_id. Plumbed into the message renderer
    // so bubble side (left vs right) can be decided by sender_id ==
    // viewer comparison for H2H, where role='user' on both peers and
    // the role-based discriminator collapses.
    val currentUserPrincipalId: String? = null,
) {
    val isHumanChat: Boolean
        get() = participantPrincipalId != null
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
    val mediaUrls: List<String> = emptyList(),
    val audioUrl: String? = null,
    val audioDurationSeconds: Int? = null,
    val createdAtMs: Long,
    val status: LocalMessageStatus,
    val isPlaceholder: Boolean,
    val errorMessage: String? = null,
    val draftForRetry: SendMessageDraft?,
    // Phase 2.7 streaming. When non-null, the bubble renders this growing content
    // and suppresses the waiting wave indicator. Replaced by the server-truth Remote
    // message on `done` per docs/SSE-PROTOCOL.md.
    val streamingBuffer: String? = null,
    // Phase 5b path lock. Computed once on the first non-empty streaming chunk based
    // on `shouldRenderAsMarkdown(firstChunk)` (ASCII check). null = not yet decided,
    // true = Markdown-rendered throughout, false = Text-rendered throughout. The lock
    // is persisted into a VM-scoped map keyed by the server `assistant_message.id` at
    // `done` so the Remote replacement renders with the same path — no Text↔Markdown
    // subtree swap.
    val useMarkdownLocked: Boolean? = null,
    // COLLAGE reference — the optimistic echo renders through the same
    // reference→GET path as Remote collage messages; URLs never live here.
    val collageId: String? = null,
    val collageBotId: String? = null,
    val collageDate: String? = null,
)
