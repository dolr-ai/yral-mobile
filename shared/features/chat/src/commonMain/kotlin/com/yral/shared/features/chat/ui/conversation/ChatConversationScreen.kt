package com.yral.shared.features.chat.ui.conversation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import co.touchlab.kermit.Logger
import com.yral.shared.analytics.events.SignupPageName
import com.yral.shared.analytics.events.SubscriptionEntryPoint
import com.yral.shared.core.session.ProDetails
import com.yral.shared.features.auth.ui.LoginBottomSheetType
import com.yral.shared.features.auth.ui.LoginMode
import com.yral.shared.features.auth.ui.LoginScreenType
import com.yral.shared.features.auth.ui.rememberLoginInfo
import com.yral.shared.features.chat.attachments.FilePathChatAttachment
import com.yral.shared.features.chat.domain.models.ChatMessageType
import com.yral.shared.features.chat.domain.models.ConversationMessageRole
import com.yral.shared.features.chat.domain.models.SendMessageDraft
import com.yral.shared.features.chat.nav.conversation.ConversationComponent
import com.yral.shared.features.chat.ui.components.ChatErrorBottomSheet
import com.yral.shared.features.chat.viewmodel.ConversationMessageItem
import com.yral.shared.features.chat.viewmodel.ConversationViewModel
import com.yral.shared.features.subscriptions.nav.SubscriptionNudgeContent
import com.yral.shared.iap.utils.getPurchaseContext
import com.yral.shared.libs.designsystem.component.YralAsyncImage
import com.yral.shared.libs.designsystem.component.YralLoader
import com.yral.shared.libs.designsystem.component.toast.ToastManager
import com.yral.shared.libs.designsystem.component.toast.ToastStatus
import com.yral.shared.libs.designsystem.component.toast.ToastType
import com.yral.shared.libs.designsystem.component.toast.showError
import com.yral.shared.libs.designsystem.component.toast.showSuccess
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.rust.service.utils.CanisterData
import com.yral.shared.rust.service.utils.getUserInfoServiceCanister
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import yral_mobile.shared.features.chat.generated.resources.Res
import yral_mobile.shared.features.chat.generated.resources.access_activated_overlay_message
import yral_mobile.shared.features.chat.generated.resources.chat_background_inverted
import yral_mobile.shared.features.chat.generated.resources.subscription_card_overlay_message
import yral_mobile.shared.features.chat.generated.resources.subscription_nudge_chat_description
import yral_mobile.shared.features.chat.generated.resources.subscription_nudge_chat_title
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

private const val EXPIRING_SOON_THRESHOLD_MS = 10L * 60 * 1000 // 10 minutes

private data class AccessExpiryDisplay(
    val text: String?,
    val isExpiringSoon: Boolean,
)

/**
 * Chat screen for testing conversation functionality.
 *
 * - Creates conversation for a hardcoded influencer
 * - Shows messages (oldest at top / newest at bottom) using LazyColumn(reverseLayout = true)
 * - Supports sending TEXT and IMAGE messages and retrying failed messages
 */
@Suppress("LongMethod", "CyclomaticComplexMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatConversationScreen(
    modifier: Modifier = Modifier,
    component: ConversationComponent,
    viewModel: ConversationViewModel = koinViewModel(),
) {
    val viewState by viewModel.viewState.collectAsState()
    val errorBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val purchaseContext = getPurchaseContext()

    val params = component.openConversationParams
    LaunchedEffect(
        params.influencerId,
        params.conversationId,
        params.userId,
        viewState.isSocialSignedIn,
    ) {
        val conversationId = params.conversationId
        val userId = params.userId
        if (conversationId != null && userId != null) {
            viewModel.initializeFromInbox(
                conversationId = conversationId,
                influencerId = params.influencerId,
                influencerCategory = params.influencerCategory,
                influencerSource = params.influencerSource,
                displayName = params.displayName,
                userName = params.username,
                avatarUrl = params.avatarUrl,
            )
        } else {
            viewModel.initializeForChatWall(
                influencerId = params.influencerId,
                influencerCategory = params.influencerCategory,
                influencerSource = params.influencerSource,
                displayName = params.displayName,
                userName = params.username,
                avatarUrl = params.avatarUrl,
            )
        }
    }

    LaunchedEffect(params.conversationId) {
        val conversationId = params.conversationId
        if (conversationId != null) {
            viewModel.markConversationAsRead(conversationId)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.influencerSubscriptionToastFlow.collect { event ->
            when (event.status) {
                ToastStatus.Success ->
                    ToastManager.showSuccess(type = ToastType.Small(message = event.message))
                ToastStatus.Error ->
                    ToastManager.showError(type = ToastType.Small(message = event.message))
                ToastStatus.Info,
                ToastStatus.Warning,
                ->
                    ToastManager.showToast(
                        type = ToastType.Small(message = event.message),
                        status = event.status,
                    )
            }
        }
    }

    // Auto-trigger purchase when navigated from Subscribe button on profile
    if (params.autoTriggerPurchase) {
        var purchaseTriggered by remember { mutableStateOf(false) }
        LaunchedEffect(viewState.influencer?.id) {
            if (viewState.influencer != null && !purchaseTriggered) {
                purchaseTriggered = true
                purchaseContext?.let { viewModel.launchInfluencerSubscriptionPurchase(it) }
            }
        }
    }

    val overlayItems by viewModel.overlay.collectAsState()
    val historyPagingItems = viewModel.history.collectAsLazyPagingItems()

    var input by remember { mutableStateOf("") }
    var selectedImage by remember { mutableStateOf<FilePathChatAttachment?>(null) }
    val listState = rememberLazyListState()
    val screenWidth = LocalWindowInfo.current.containerSize.width
    val density = LocalDensity.current

    val imagePickerLauncher =
        rememberChatImagePicker(
            onImagePicked = { attachment -> selectedImage = attachment },
        )
    val imageCaptureLauncher =
        rememberChatImageCapture(
            onImagePicked = { attachment -> selectedImage = attachment },
        )

    var readyForAutoScroll by remember { mutableStateOf(false) }
    LaunchedEffect(input) {
        if (input.isNotBlank()) {
            readyForAutoScroll = true
        }
    }

    val latestAssistantState by derivedStateOf {
        findLatestAssistantIndex(
            overlayItems = overlayItems,
            historyPagingItems = historyPagingItems,
        )
    }

    // Auto-scroll to show last line of new assistant replies
    // Standard line height for "text" message content mapped to Markdown typography
    val messageLineHeightPx =
        with(density) {
            LocalAppTopography.current.baseRegular.lineHeight
                .toPx()
        }
    AutoScrollToAssistantMessage(
        readyForAutoScroll = readyForAutoScroll,
        latestAssistantMessage = latestAssistantState.value,
        targetIndex = latestAssistantState.index,
        listState = listState,
        screenWidth = screenWidth,
        density = density,
        overlayItems = overlayItems,
        scrollToLastLine = true,
        lineHeightPx = messageLineHeightPx,
    )

    // Check if there's a waiting assistant message in overlay
    val hasWaitingAssistant by derivedStateOf { overlayItems.any { it.isWaitingAssistant() } }

    val overlaySentCount by derivedStateOf { overlayItems.count { it is ConversationMessageItem.Remote } }
    val totalMessageCount by derivedStateOf { viewState.totalHistoryMessageCount + overlaySentCount }

    val proDetails by component.subscriptionCoordinator.proDetails.collectAsStateWithLifecycle(ProDetails())
    val shouldPromptForLogin by derivedStateOf {
        !viewState.isSocialSignedIn && totalMessageCount >= viewState.loginPromptMessageThreshold
    }
    val hasChatAccess by derivedStateOf {
        proDetails.isProPurchased || viewState.isInfluencerSubscriptionPurchasedAndVerified
    }
    val atSubscriptionThreshold by derivedStateOf {
        totalMessageCount >= viewState.subscriptionMandatoryThreshold
    }
    val shouldShowInfluencerSubscriptionCard by derivedStateOf {
        val result =
            !hasWaitingAssistant &&
                viewState.isSocialSignedIn &&
                viewState.isSubscriptionEnabled &&
                !hasChatAccess &&
                atSubscriptionThreshold &&
                viewState.isInfluencerSubscriptionAvailableToPurchase
        Logger.d("SubDebug") {
            "shouldShow=$result | waiting=$hasWaitingAssistant | signed=${viewState.isSocialSignedIn}" +
                " | subEnabled=${viewState.isSubscriptionEnabled} | access=$hasChatAccess" +
                " | threshold=$atSubscriptionThreshold(count=$totalMessageCount/" +
                "${viewState.subscriptionMandatoryThreshold})" +
                " | productAvail=${viewState.isInfluencerSubscriptionAvailableToPurchase}" +
                " | loading=${viewState.isChatAccessLoading}"
        }
        result
    }
    val shouldShowSubscriptionNudge by derivedStateOf {
        viewState.isSocialSignedIn &&
            viewState.isSubscriptionEnabled &&
            !hasChatAccess &&
            atSubscriptionThreshold &&
            !viewState.isInfluencerSubscriptionAvailableToPurchase &&
            viewState.isYralProAvailableToPurchase
    }
    val shouldBlockChatNoProduct by derivedStateOf {
        viewState.isSocialSignedIn &&
            viewState.isSubscriptionEnabled &&
            !hasChatAccess &&
            atSubscriptionThreshold &&
            !viewState.isInfluencerSubscriptionAvailableToPurchase &&
            !viewState.isYralProAvailableToPurchase
    }

    val subscriptionCardOverlayMessage =
        if (shouldShowInfluencerSubscriptionCard) {
            stringResource(Res.string.subscription_card_overlay_message)
        } else {
            null
        }
    val accessActivatedOverlayMessage =
        if (viewState.isInfluencerSubscriptionPurchasedAndVerified) {
            stringResource(Res.string.access_activated_overlay_message)
        } else {
            null
        }
    LaunchedEffect(subscriptionCardOverlayMessage, accessActivatedOverlayMessage) {
        viewModel.setSystemOverlayMessages(
            subscriptionCardMessage = subscriptionCardOverlayMessage,
            accessActivatedMessage = accessActivatedOverlayMessage,
        )
    }
    LaunchedEffect(shouldShowInfluencerSubscriptionCard, viewState.influencer?.id) {
        if (shouldShowInfluencerSubscriptionCard) {
            viewState.influencer?.id?.let { viewModel.trackFreeAccessExpired(it) }
        }
    }

    val loginState =
        rememberLoginInfo(
            requestLoginFactory = component.requestLoginFactory,
            key = viewState.influencer,
        )
    val promptLogin: () -> Unit =
        remember(viewState.influencer) {
            {
                val influencer = viewState.influencer
                val influencerName = influencer?.displayName ?: ""
                val influencerAvatarUrl = influencer?.avatarUrl ?: ""
                loginState.requestLogin(
                    SignupPageName.CONVERSATION,
                    LoginScreenType.BottomSheet(
                        LoginBottomSheetType.CONVERSATION(
                            influencerName = influencerName,
                            influencerAvatarUrl = influencerAvatarUrl,
                        ),
                    ),
                    LoginMode.BOTH,
                    null,
                    null,
                ) {}
            }
        }

    val subscriptionNudgeTitle =
        stringResource(
            Res.string.subscription_nudge_chat_title,
            viewState.influencer?.displayName ?: "",
        )
    val subscriptionNudgeDescription =
        stringResource(
            Res.string.subscription_nudge_chat_description,
            viewState.influencer?.displayName ?: "",
            proDetails.totalCredits,
        )
    val subscriptionNudgeContent =
        remember(subscriptionNudgeTitle, subscriptionNudgeDescription, viewState.influencer) {
            SubscriptionNudgeContent(
                title = subscriptionNudgeTitle,
                description = subscriptionNudgeDescription,
                topContent = {
                    viewState.influencer?.avatarUrl?.let { avatarUrl ->
                        YralAsyncImage(
                            imageUrl = avatarUrl,
                            modifier =
                                Modifier
                                    .padding(0.dp)
                                    .width(120.dp)
                                    .height(120.dp),
                        )
                    }
                },
                entryPoint = SubscriptionEntryPoint.AI_CHATBOT,
            )
        }

    fun sendMessageIfAllowed(
        draft: SendMessageDraft,
        onBeforeSend: () -> Unit = {},
    ) {
        val blocked =
            when {
                shouldPromptForLogin -> {
                    promptLogin()
                    true
                }
                shouldShowInfluencerSubscriptionCard -> {
                    purchaseContext?.let { viewModel.launchInfluencerSubscriptionPurchase(it) }
                    true
                }
                shouldShowSubscriptionNudge -> {
                    component.subscriptionCoordinator.showSubscriptionNudge(content = subscriptionNudgeContent)
                    true
                }
                shouldBlockChatNoProduct -> {
                    viewModel.showPurchaseUnavailableToast()
                    true
                }
                else -> false
            }
        if (!blocked) {
            onBeforeSend()
            viewModel.sendMessage(draft)
        }
    }

    Box(modifier = modifier.safeDrawingPadding().clipToBounds()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .paint(
                        painter = painterResource(Res.drawable.chat_background_inverted),
                        contentScale = ContentScale.Crop,
                    ),
        ) {
            val expiresAtMs = viewState.chatAccessExpiresAtMs
            val showAccessExpiry =
                viewState.isInfluencerSubscriptionPurchasedAndVerified && expiresAtMs != null
            val accessExpiryDisplay by produceState(
                initialValue = AccessExpiryDisplay(null, false),
                expiresAtMs,
                showAccessExpiry,
            ) {
                if (!showAccessExpiry) {
                    value = AccessExpiryDisplay(null, false)
                    return@produceState
                }
                while (true) {
                    val remaining = remainingAccessMs(expiresAtMs)
                    val text = if (remaining <= 0L) null else formatMillisToHHmmSS(remaining)
                    val isExpiringSoon = remaining in 1..EXPIRING_SOON_THRESHOLD_MS
                    value = AccessExpiryDisplay(text, isExpiringSoon)
                    delay(1.seconds)
                }
            }
            val showHeaderSubscribe by derivedStateOf {
                viewState.isSocialSignedIn &&
                    viewState.isSubscriptionEnabled &&
                    !viewState.isBotAccount &&
                    viewState.isInfluencerSubscriptionAvailableToPurchase &&
                    !viewState.isInfluencerSubscriptionPurchasedAndVerified
            }
            // Header
            ChatHeader(
                influencer = viewState.influencer,
                onBackClick = { component.onBack() },
                onProfileClick = { influencer ->
                    val userPrincipal =
                        if (viewState.isBotAccount) {
                            component.openConversationParams.userId
                        } else {
                            influencer.id
                        }
                    if (userPrincipal == null) return@ChatHeader
                    val canisterData =
                        CanisterData(
                            canisterId = getUserInfoServiceCanister(),
                            userPrincipalId = userPrincipal,
                            profilePic = component.openConversationParams.avatarUrl.orEmpty(),
                            username = component.openConversationParams.username.orEmpty(),
                            isCreatedFromServiceCanister = true,
                            isFollowing = false,
                        )
                    component.openProfile(canisterData)
                },
                onClearChat = { viewModel.deleteAndRecreateConversation(params.influencerId) },
                onShareProfile = { viewModel.shareProfile() },
                accessExpiresInText = accessExpiryDisplay.text,
                isAccessExpiringSoon = accessExpiryDisplay.isExpiringSoon,
                isBotAccount = viewState.isBotAccount,
                showSubscribe = showHeaderSubscribe,
                isSubscribeLoading = viewState.isInfluencerSubscriptionPurchaseInProgress,
                onSubscribeClick = {
                    purchaseContext?.let { viewModel.launchInfluencerSubscriptionPurchase(it) }
                },
            )

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
            ) {
                when {
                    viewState.isCreating -> {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            YralLoader()
                        }
                    }

                    else -> {
                        MessagesList(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            listState = listState,
                            overlayItems = overlayItems,
                            historyPagingItems = historyPagingItems,
                            isBotAccount = viewState.isBotAccount,
                            onRetry = { localId -> viewModel.retry(localId) },
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Check if there are any user messages
                        // Only check overlay items and snapshot of loaded history items for performance
                        val hasUserMessages by derivedStateOf {
                            overlayItems.any { item ->
                                when (item) {
                                    is ConversationMessageItem.Local ->
                                        item.message.role == ConversationMessageRole.USER
                                    is ConversationMessageItem.Remote ->
                                        item.message.role == ConversationMessageRole.USER
                                }
                            }
                        }
                        val suggestions = viewState.influencer?.suggestedMessages.orEmpty()
                        val shouldShowSuggestions = !hasUserMessages && suggestions.isNotEmpty()
                        // Show suggestion messages if there are no user messages
                        if (shouldShowSuggestions) {
                            SuggestionMessagesColumn(
                                suggestions = suggestions,
                                onSuggestionClick = { suggestion ->
                                    sendMessageIfAllowed(
                                        SendMessageDraft(
                                            messageType = ChatMessageType.TEXT,
                                            content = suggestion,
                                        ),
                                    )
                                },
                                modifier = Modifier.padding(bottom = 12.dp),
                            )
                        }

                        // Influencer subscription card (replaces input when at threshold) or ChatInputArea
                        if (shouldShowInfluencerSubscriptionCard && !viewState.isBotAccount) {
                            InfluencerSubscriptionCard(
                                onSubscribe = {
                                    purchaseContext?.let { viewModel.launchInfluencerSubscriptionPurchase(it) }
                                },
                                isPurchaseInProgress = viewState.isInfluencerSubscriptionPurchaseInProgress,
                                formattedPrice = viewState.influencerSubscriptionFormattedPrice,
                            )
                        } else if (shouldBlockChatNoProduct && !viewState.isBotAccount) {
                            InfluencerSubscriptionCard(
                                onSubscribe = { viewModel.showPurchaseUnavailableToast() },
                                isPurchaseInProgress = false,
                                formattedPrice = null,
                            )
                        } else if (!viewState.isBotAccount) {
                            ChatInputArea(
                                input = input,
                                onInputChange = { input = it },
                                onSendClick = {
                                    val text = input.trim()
                                    sendMessageIfAllowed(
                                        SendMessageDraft(
                                            messageType = ChatMessageType.TEXT,
                                            content = text,
                                        ),
                                    ) {
                                        input = ""
                                    }
                                },
                                onCameraClick = imageCaptureLauncher,
                                onGalleryClick = imagePickerLauncher,
                                hasWaitingAssistant = hasWaitingAssistant,
                            )
                        }
                    }
                }
            }
        }

        // Image preview overlay (fullscreen, on top of everything including message list)
        selectedImage?.let { imageAttachment ->
            ImagePreviewOverlay(
                imageAttachment = imageAttachment,
                onSend = { draft ->
                    sendMessageIfAllowed(draft) {
                        selectedImage = null
                    }
                },
                onDismiss = { selectedImage = null },
                hasWaitingAssistant = hasWaitingAssistant,
                modifier = Modifier.fillMaxSize(),
            )
        }

        viewState.chatError?.let { errorToShow ->
            ChatErrorBottomSheet(
                error = errorToShow,
                bottomSheetState = errorBottomSheetState,
                onDismissRequest = { viewModel.clearError() },
            )
        }
    }
}

@Suppress("MagicNumber")
internal fun formatMillisToHHmmSS(millis: Long): String {
    val duration = millis.milliseconds
    val hours = duration.inWholeHours
    val minutes = duration.inWholeMinutes % 60
    val seconds = duration.inWholeSeconds % 60

    fun Long.twoDigits(): String = this.toString().padStart(2, '0')

    return "${hours.twoDigits()}:${minutes.twoDigits()}:${seconds.twoDigits()}"
}

@OptIn(ExperimentalTime::class)
private fun remainingAccessMs(expiresAtMs: Long?): Long {
    if (expiresAtMs == null) return 0L
    val nowMs = Clock.System.now().toEpochMilliseconds()
    return expiresAtMs - nowMs
}
