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
import com.yral.shared.analytics.events.InfluencerSource
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.core.session.SessionManager
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.features.chat.analytics.ChatTelemetry
import com.yral.shared.features.chat.attachments.ChatAttachment
import com.yral.shared.features.chat.attachments.FilePathChatAttachment
import com.yral.shared.features.chat.domain.ChatErrorMapper
import com.yral.shared.features.chat.domain.ChatRepository
import com.yral.shared.features.chat.domain.ConversationMessagesPagingSource
import com.yral.shared.features.chat.domain.EmptyMessagesPagingSource
import com.yral.shared.features.chat.domain.models.ChatError
import com.yral.shared.features.chat.domain.models.ChatMessage
import com.yral.shared.features.chat.domain.models.ChatMessageType
import com.yral.shared.features.chat.domain.models.ConversationInfluencer
import com.yral.shared.features.chat.domain.models.ConversationMessageRole
import com.yral.shared.features.chat.domain.models.SendMessageDraft
import com.yral.shared.features.chat.domain.models.SendMessageResult
import com.yral.shared.features.chat.domain.usecases.CreateConversationUseCase
import com.yral.shared.features.chat.domain.usecases.DeleteConversationUseCase
import com.yral.shared.features.chat.domain.usecases.SendMessageUseCase
import com.yral.shared.iap.IAPManager
import com.yral.shared.iap.PurchaseResult
import com.yral.shared.iap.core.IAPError
import com.yral.shared.iap.core.model.ProductId
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
import kotlinx.coroutines.channels.Channel
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
    private val shareService: ShareService,
    private val urlBuilder: UrlBuilder,
    private val linkGenerator: LinkGenerator,
    private val crashlyticsManager: CrashlyticsManager,
    private val sessionManager: SessionManager,
    private val chatTelemetry: ChatTelemetry,
    private val chatErrorMapper: ChatErrorMapper,
    private val iapManager: IAPManager,
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
                subscriptionAllowedInfluencerId =
                    flagManager.get(ChatFeatureFlags.Chat.SubscriptionAllowedInfluencerId),
            ),
        )
    val viewState: StateFlow<ConversationViewState> = _viewState.asStateFlow()

    private val influencerSubscriptionToastChannel = Channel<InfluencerSubscriptionToastEvent>(Channel.CONFLATED)
    val influencerSubscriptionToastFlow = influencerSubscriptionToastChannel.receiveAsFlow()

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
                accessActivatedMessage != null ->
                    listOf(
                        createSystemSentMessage(
                            now,
                            convId,
                            createdAt,
                            "system-access-activated",
                            accessActivatedMessage,
                        ),
                    )
                subscriptionCardMessage != null ->
                    listOf(
                        createSystemSentMessage(
                            now,
                            convId,
                            createdAt,
                            "system-free-messages-over",
                            subscriptionCardMessage,
                        ),
                    )
                else -> emptyList()
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
        val allowedId = _viewState.value.subscriptionAllowedInfluencerId
        val isAllowedInfluencer = allowedId.isNotBlank() && influencerId == allowedId
        if (isAllowedInfluencer) {
            fetchInfluencerSubscriptionAndYralProProducts()
        } else {
            clearInfluencerSubscriptionAndFetchYralProOnly()
        }
    }

    private fun fetchInfluencerSubscriptionAndYralProProducts() {
        viewModelScope.launch {
            runSuspendCatching {
                iapManager.isProductPurchased(ProductId.TARA_SUBSCRIPTION)
            }.onSuccess { result ->
                val purchaseResult = result.getOrNull()
                val isPurchasedAndVerified = purchaseResult is PurchaseResult.PurchaseMatches
                val purchaseTimeMs = (purchaseResult as? PurchaseResult.PurchaseMatches)?.purchaseTime
                _viewState.update {
                    it.copy(
                        isInfluencerSubscriptionPurchasedAndVerified = isPurchasedAndVerified,
                        influencerSubscriptionPurchaseTimeMs = if (isPurchasedAndVerified) purchaseTimeMs else null,
                    )
                }
            }.onFailure {
                _viewState.update {
                    it.copy(
                        isInfluencerSubscriptionPurchasedAndVerified = false,
                        influencerSubscriptionPurchaseTimeMs = null,
                    )
                }
            }
        }
        viewModelScope.launch {
            runSuspendCatching {
                iapManager.fetchProducts(listOf(ProductId.TARA_SUBSCRIPTION, ProductId.YRAL_PRO))
            }.onSuccess { result ->
                val products = result.getOrNull().orEmpty()
                val productIds = products.map { it.id }.toSet()
                val influencerAvailable =
                    ProductId.TARA_SUBSCRIPTION.productId in productIds
                val yralProAvailable = ProductId.YRAL_PRO.productId in productIds
                val influencerOfferPrice =
                    products
                        .find { it.id == ProductId.TARA_SUBSCRIPTION.productId }
                        ?.let { p -> p.offerPrice.ifBlank { p.price } }
                _viewState.update {
                    it.copy(
                        isInfluencerSubscriptionAvailableToPurchase = influencerAvailable,
                        isYralProAvailableToPurchase = yralProAvailable,
                        influencerSubscriptionFormattedPrice = influencerOfferPrice,
                    )
                }
            }.onFailure {
                _viewState.update {
                    it.copy(
                        isInfluencerSubscriptionAvailableToPurchase = false,
                        isYralProAvailableToPurchase = false,
                        influencerSubscriptionFormattedPrice = null,
                    )
                }
            }
        }
    }

    private fun clearInfluencerSubscriptionAndFetchYralProOnly() {
        _viewState.update {
            it.copy(
                isInfluencerSubscriptionPurchasedAndVerified = false,
                isInfluencerSubscriptionAvailableToPurchase = false,
                influencerSubscriptionFormattedPrice = null,
                influencerSubscriptionPurchaseTimeMs = null,
            )
        }
        viewModelScope.launch {
            runSuspendCatching {
                iapManager.fetchProducts(listOf(ProductId.YRAL_PRO))
            }.onSuccess { result ->
                val products = result.getOrNull().orEmpty()
                val yralProAvailable = ProductId.YRAL_PRO.productId in products.map { it.id }.toSet()
                _viewState.update { it.copy(isYralProAvailableToPurchase = yralProAvailable) }
            }.onFailure {
                _viewState.update { it.copy(isYralProAvailableToPurchase = false) }
            }
        }
    }

    fun trackFreeAccessExpired(botId: String) {
        chatTelemetry.freeAccessExpired(botId)
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

    private suspend fun performInfluencerSubscriptionPurchase(
        purchaseContext: PurchaseContext,
        botId: String?,
    ) {
        runSuspendCatching {
            iapManager.purchaseProduct(
                productId = ProductId.TARA_SUBSCRIPTION,
                context = purchaseContext,
                acknowledgePurchase = false,
            )
        }.onSuccess {
            runSuspendCatching {
                iapManager.isProductPurchased(ProductId.TARA_SUBSCRIPTION)
            }.onSuccess { result ->
                val purchase = result.getOrNull()
                val isPurchased = purchase is PurchaseResult.PurchaseMatches
                val purchaseTime = (purchase as? PurchaseResult.PurchaseMatches)?.purchaseTime
                if (purchase == null || purchase is PurchaseResult.NoPurchase) {
                    botId?.let { chatTelemetry.subscriptionFailed(it, "no_purchase") }
                    _viewState.update { it.copy(isInfluencerSubscriptionPurchaseInProgress = false) }
                    return@onSuccess
                }
                handleInfluencerSubscriptionVerificationResult(
                    isPurchased = isPurchased,
                    purchaseTimeMs = purchaseTime,
                )
            }.onFailure { verificationError ->
                _viewState.update { it.copy(isInfluencerSubscriptionPurchaseInProgress = false) }
                botId?.let {
                    chatTelemetry.subscriptionFailed(it, verificationError.message ?: "unknown")
                }
            }
        }.onFailure { error ->
            _viewState.update { it.copy(isInfluencerSubscriptionPurchaseInProgress = false) }
            botId?.let { chatTelemetry.subscriptionFailed(it, error.message ?: "unknown") }
            when (error) {
                is IAPError.PurchaseCancelled -> return@onFailure
                else -> {
                    val message =
                        when (error) {
                            is IAPError.PurchasePending ->
                                getString(Res.string.influencer_subscription_purchase_pending)
                            else ->
                                getString(Res.string.influencer_subscription_purchase_failed)
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
        purchaseTimeMs: Long?,
    ) {
        _viewState.update {
            it.copy(
                isInfluencerSubscriptionPurchasedAndVerified = isPurchased,
                isInfluencerSubscriptionPurchaseInProgress = false,
                influencerSubscriptionPurchaseTimeMs = if (isPurchased) purchaseTimeMs else null,
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
                            influencer
                                .suggestedMessages
                                .ifEmpty { existingInfluencer.suggestedMessages },
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
                totalHistoryMessageCount = messageCount,
            )
        }

        if (resolvedInfluencer != null) {
            updateInfluencerSubscriptionProductState(resolvedInfluencer.id)
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

    fun initializeForInfluencer(
        influencerId: String,
        influencerCategory: String,
        influencerSource: InfluencerSource = InfluencerSource.CARD,
    ) {
        _viewState.update { it.copy(influencerSource = influencerSource) }
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
                paginatedHistoryAvailable = false,
                shareDisplayName = "",
                shareMessage = "",
                shareDescription = "",
                isSocialSignedIn = current.isSocialSignedIn,
                influencerSource = current.influencerSource,
                loginPromptMessageThreshold = current.loginPromptMessageThreshold,
                subscriptionMandatoryThreshold = current.subscriptionMandatoryThreshold,
                isSubscriptionEnabled = current.isSubscriptionEnabled,
                isInfluencerSubscriptionPurchasedAndVerified = current.isInfluencerSubscriptionPurchasedAndVerified,
                isInfluencerSubscriptionAvailableToPurchase = current.isInfluencerSubscriptionAvailableToPurchase,
                isYralProAvailableToPurchase = current.isYralProAvailableToPurchase,
                isInfluencerSubscriptionPurchaseInProgress = false,
                influencerSubscriptionFormattedPrice = current.influencerSubscriptionFormattedPrice,
                subscriptionAllowedInfluencerId = current.subscriptionAllowedInfluencerId,
                totalHistoryMessageCount = 0,
            )
        }
        _overlay.value = OverlayState()
        systemOverlayMessagesFlow.value = emptyList()
        loadedMessageIds.value = emptySet()
        initialOffset.value = null
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

    companion object {
        private const val PAGE_SIZE = 10
        private const val PREFETCH_DISTANCE = 5
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
    val paginatedHistoryAvailable: Boolean = false,
    val shareDisplayName: String = "",
    val shareMessage: String = "",
    val shareDescription: String = "",
    val isSocialSignedIn: Boolean = false,
    val influencerSource: InfluencerSource = InfluencerSource.CARD,
    val loginPromptMessageThreshold: Int,
    val subscriptionMandatoryThreshold: Int,
    val isSubscriptionEnabled: Boolean,
    val isInfluencerSubscriptionPurchasedAndVerified: Boolean = false,
    val isInfluencerSubscriptionAvailableToPurchase: Boolean = false,
    val isYralProAvailableToPurchase: Boolean = false,
    val isInfluencerSubscriptionPurchaseInProgress: Boolean = false,
    val influencerSubscriptionFormattedPrice: String? = null,
    val subscriptionAllowedInfluencerId: String = "",
    val totalHistoryMessageCount: Int = 0,
    val influencerSubscriptionPurchaseTimeMs: Long? = null,
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
