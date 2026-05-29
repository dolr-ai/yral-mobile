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
import com.yral.shared.features.chat.domain.ChatErrorMapper
import com.yral.shared.features.chat.data.ConversationContentCache
import com.yral.shared.features.chat.domain.ChatRepository
import com.yral.shared.features.chat.domain.ConversationMessagesPagingSource
import com.yral.shared.features.chat.domain.EmptyMessagesPagingSource
import com.yral.shared.features.chat.domain.models.AssistantErrorPresentation
import com.yral.shared.features.chat.domain.models.ChatError
import com.yral.shared.features.chat.domain.models.ChatMessage
import com.yral.shared.features.chat.domain.models.ChatMessageType
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
import com.yral.shared.features.chat.domain.usecases.GrantChatAccessParams
import com.yral.shared.features.chat.domain.usecases.GrantChatAccessUseCase
import com.yral.shared.features.chat.domain.usecases.MarkConversationAsReadUseCase
import com.yral.shared.features.chat.domain.usecases.ReleaseHumanCreatorTakeoverUseCase
import com.yral.shared.features.chat.domain.usecases.SendHumanCreatorMessageUseCase
import com.yral.shared.features.chat.domain.usecases.SendMessageUseCase
import com.yral.shared.features.chat.domain.usecases.StartHumanCreatorTakeoverUseCase
import com.yral.shared.features.chat.ui.conversation.shouldRenderAsMarkdown
import com.yral.shared.features.subscriptions.domain.FetchProductsUseCase
import com.yral.shared.iap.IAPManager
import com.yral.shared.iap.core.IAPError
import com.yral.shared.iap.core.model.ProductId
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import yral_mobile.shared.features.chat.generated.resources.Res
import yral_mobile.shared.features.chat.generated.resources.influencer_subscription_purchase_failed
import yral_mobile.shared.features.chat.generated.resources.influencer_subscription_purchase_pending
import yral_mobile.shared.features.chat.generated.resources.influencer_subscription_purchase_unavailable
import yral_mobile.shared.features.chat.generated.resources.influencer_subscription_unlocked_toast
import yral_mobile.shared.features.chat.generated.resources.influencer_subscription_verification_pending
import yral_mobile.shared.libs.designsystem.generated.resources.msg_profile_share
import yral_mobile.shared.libs.designsystem.generated.resources.msg_profile_share_desc
import yral_mobile.shared.libs.designsystem.generated.resources.profile_share_default_name
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@Suppress("LongParameterList", "TooManyFunctions", "LargeClass")
class ConversationViewModel(
    flagManager: FeatureFlagManager,
    private val chatRepository: ChatRepository,
    private val useCaseFailureListener: UseCaseFailureListener,
    private val sendMessageUseCase: SendMessageUseCase,
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
                subscriptionMandatoryThreshold = flagManager.get(ChatFeatureFlags.Chat.SubscriptionMandatoryThreshold),
                isSubscriptionEnabled = flagManager.isEnabled(AppFeatureFlags.Common.EnableSubscription),
                isChatAsHumanCreatorEnabled = flagManager.get(ChatFeatureFlags.Chat.ChatAsHumanCreatorEnabled),
                isSseStreamingEnabled = flagManager.get(ChatFeatureFlags.Chat.SseStreamingEnabled),
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

    private val systemOverlayMessagesFlow = MutableStateFlow<List<SentMessage>>(emptyList())

    val overlay: StateFlow<List<ConversationMessageItem>> =
        combine(_overlay, loadedMessageIds, systemOverlayMessagesFlow) { overlayState, loadedIds, systemMessages ->
            val filteredSent = overlayState.sent.filterNot { it.message.id in loadedIds }
            buildList {
                overlayState.pending.forEach { pending ->
                    add(pending.createdAtMs to ConversationMessageItem.Local(pending))
                }
                filteredSent.forEach { sent ->
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
            }
                .debounce(CACHE_WRITE_DEBOUNCE_MS)
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
                        retryUnconsumedDailyChatAccess(influencerId)
                    }
                }.onFailure { error ->
                    Logger.e("SubscriptionX", error) { "checkChatAccess failed" }
                    _viewState.update { it.copy(isChatAccessLoading = false) }
                    fetchInfluencerSubscriptionProducts()
                }
        }
    }

    private suspend fun retryUnconsumedDailyChatAccess(botId: String) {
        runSuspendCatching {
            iapManager.restorePurchases()
        }.onSuccess { restoreResult ->
            val purchases = restoreResult.getOrNull()?.purchases.orEmpty()
            val unconsumedDailyChat =
                purchases.firstOrNull { purchase ->
                    purchase.productId == ProductId.DAILY_CHAT &&
                        purchase.state == PurchaseState.PURCHASED
                }

            if (unconsumedDailyChat?.purchaseToken != null) {
                Logger.d("SubscriptionX") { "Found unconsumed daily_chat, retrying grant..." }
                retryGrantAccess(botId, unconsumedDailyChat, ProductId.DAILY_CHAT.productId)
            } else {
                _viewState.update { it.copy(isChatAccessLoading = false) }
                fetchInfluencerSubscriptionProducts()
            }
        }.onFailure {
            Logger.e("SubscriptionX", it) { "Daily Chat restore failed" }
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
            handleGrantFailure(error, purchaseToken, purchase.purchaseTime)
        }
    }

    private suspend fun handleGrantFailure(
        error: Throwable,
        purchaseToken: String,
        purchaseTime: Long?,
    ) {
        when (error) {
            is GrantError.ClientError -> {
                // 400: Token rejected (wrong bot, expired, cancelled). Consume to stop retry loop.
                Logger.w("SubscriptionX") { "Grant rejected (400): ${error.errorMsg}. Consuming purchase." }
                consumePurchaseInBackground(purchaseToken)
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
        viewModelScope.launch {
            fetchProductsUseCase(listOf(ProductId.DAILY_CHAT))
                .onSuccess { products ->
                    val influencerAvailable = ProductId.DAILY_CHAT.productId in products.map { it.id }.toSet()
                    val influencerOfferPrice =
                        products
                            .find { it.id == ProductId.DAILY_CHAT.productId }
                            ?.let { p -> p.offerPrice.ifBlank { p.price } }
                    _viewState.update {
                        it.copy(
                            isInfluencerSubscriptionAvailableToPurchase = influencerAvailable,
                            influencerSubscriptionFormattedPrice = influencerOfferPrice,
                        )
                    }
                }.onFailure {
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
        // Check for unconsumed DAILY_CHAT from a previous failed grant
        val existingPurchase = findUnconsumedDailyChat()
        if (existingPurchase != null && botId != null) {
            Logger.d("SubscriptionX") { "Found unconsumed DAILY_CHAT, retrying grant instead of new purchase" }
            retryGrantAccess(botId, existingPurchase, ProductId.DAILY_CHAT.productId)
            _viewState.update { it.copy(isInfluencerSubscriptionPurchaseInProgress = false) }
            return
        }

        runSuspendCatching {
            iapManager.purchaseProduct(
                productId = ProductId.DAILY_CHAT,
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
                        productId = ProductId.DAILY_CHAT.productId,
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
                    handleGrantFailure(grantError, purchaseToken, purchaseTime)
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

        if (recentMessages.isNotEmpty() && paginatedHistoryAvailable) {
            initialOffset.value = recentMessages.size
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
    ) {
        val isBotAccount = sessionManager.isBotAccount == true
        _viewState.update { it.copy(influencerSource = influencerSource, isBotAccount = isBotAccount) }
        val currentConversationId = _viewState.value.conversationId
        if (currentConversationId == conversationId) return
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
        updateInfluencerSubscriptionProductState(influencerId)
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
        _viewState.update { it.copy(influencerSource = influencerSource, isBotAccount = isBotAccount) }
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
                subscriptionMandatoryThreshold = current.subscriptionMandatoryThreshold,
                isSubscriptionEnabled = current.isSubscriptionEnabled,
                isChatAsHumanCreatorEnabled = current.isChatAsHumanCreatorEnabled,
                isSseStreamingEnabled = current.isSseStreamingEnabled,
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
        stopTakeoverPolling()
        stopCountdownTicker()
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

    @OptIn(ExperimentalTime::class)
    fun sendMessage(draft: SendMessageDraft) {
        val convId = conversationId ?: return

        // Phase 6: a fresh send supersedes any error from the previous send.
        // Clear before any new state lands so the user sees their new message
        // and the streaming placeholder, not a stale error bubble.
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

        if (shouldStream(draft)) {
            val streamingPlaceholder = createStreamingAssistantPlaceholder(now)
            _overlay.update { it.copy(pending = it.pending + userLocal + streamingPlaceholder) }
            startStreamingAssistantReply(
                conversationId = convId,
                draft = draft,
                streamingLocalId = streamingPlaceholder.localId,
                userLocalId = localUserId,
            )
        } else {
            val assistantPlaceholder = createAssistantPlaceholder(now)
            _overlay.update { it.copy(pending = it.pending + userLocal + assistantPlaceholder) }
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
    fun retryFailedAssistantReply() {
        val presentation = _assistantError.value ?: return
        val draft = presentation.retryDraft ?: return
        val convId = conversationId ?: return
        val userLocalId =
            _overlay.value.pending
                .lastOrNull { it.role == ConversationMessageRole.USER }
                ?.localId
                ?: return

        val now = Clock.System.now().toEpochMilliseconds()
        val streamingPlaceholder = createStreamingAssistantPlaceholder(now)
        _overlay.update { state ->
            state.copy(pending = state.pending + streamingPlaceholder)
        }
        startStreamingAssistantReply(
            conversationId = convId,
            draft = draft,
            streamingLocalId = streamingPlaceholder.localId,
            userLocalId = userLocalId,
        )
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
            !s.isHumanCreatorTakeoverActive
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

    @OptIn(ExperimentalTime::class)
    private fun startStreamingAssistantReply(
        conversationId: String,
        draft: SendMessageDraft,
        streamingLocalId: String,
        userLocalId: String,
    ) {
        viewModelScope.launch {
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
                val pendingTokenText = StringBuilder()
                var flushJob: Job? = null

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

                chatRepository
                    .streamMessage(conversationId = conversationId, draft = draft)
                    .collect { event ->
                        when (event) {
                            is StreamEvent.Token -> {
                                Logger.i("SSE") { "token len=${event.text.length} text=${event.text}" }
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
                                Logger.i("SSE") {
                                    "done id=${event.assistantMessage.id} blocked=${event.blocked} " +
                                        "createdAt=${event.assistantMessage.createdAt}"
                                }
                                val msg = event.assistantMessage
                                val insertedAtMs = parseTimestampToEpochMs(msg.createdAt)
                                Logger.i("SSE") { "done parsedInsertedAtMs=$insertedAtMs (null means parse failed)" }
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
                                // NOTE: do NOT pre-add msg.id to loadedMessageIds here. The combine
                                // block's `filteredSent = state.sent.filterNot { id in loadedIds }`
                                // would immediately hide the Remote we just inserted, since loadedIds
                                // is the "already paged in" set. The natural paging cycle adds the
                                // id once paging hydrates the message; that's when dedup kicks in.

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
                                val assistantError = event.error
                                Logger.w(ConversationViewModel::class.simpleName!!) {
                                    "Streaming failed: code=${assistantError.code} " +
                                        "rawCode=${assistantError.rawCode} " +
                                        "message=${assistantError.message} " +
                                        "retryable=${assistantError.retryable} conv=$conversationId"
                                }
                                _overlay.update { state ->
                                    state.copy(
                                        pending = state.pending.filterNot { it.localId == streamingLocalId },
                                    )
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
                Logger.w(ConversationViewModel::class.simpleName!!, error) {
                    "Streaming connection failed conv=$conversationId"
                }
                // Phase 1/3 known-issue: no fallback to legacy endpoint yet. Phase 9 carve-outs.
                _overlay.update { state ->
                    state.copy(pending = state.pending.filterNot { it.localId == streamingLocalId })
                }
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

    @Suppress("TooGenericExceptionCaught")
    private suspend fun findUnconsumedDailyChat(): Purchase? =
        try {
            iapManager
                .restorePurchases()
                .getOrNull()
                ?.purchases
                ?.firstOrNull { purchase ->
                    purchase.productId == ProductId.DAILY_CHAT && purchase.state == PurchaseState.PURCHASED
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
        private val TAKEOVER_MESSAGES_POLL_INTERVAL = 3.seconds

        // Status poll runs every Nth iteration of message poll: 3s × 2 ≈ 6s ≈ ~5s spec.
        private const val TAKEOVER_STATUS_POLL_EVERY_N = 2
        private const val TAKEOVER_POLL_PAGE_SIZE = 20
    }
}

data class InfluencerSubscriptionToastEvent(
    val status: ToastStatus,
    val message: String,
)

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
    val isHumanCreatorTakeoverActive: Boolean = false,
    val isHumanCreatorTakeoverStarting: Boolean = false,
    val isHumanCreatorTakeoverEnding: Boolean = false,
    val isHumanCreatorMessageSending: Boolean = false,
    val humanCreatorTakeoverRemainingSeconds: Int = 0,
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
)
