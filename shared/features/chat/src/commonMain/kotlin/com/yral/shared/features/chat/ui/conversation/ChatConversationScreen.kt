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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import com.yral.shared.analytics.events.SignupPageName
import com.yral.shared.features.auth.ui.LoginBottomSheetType
import com.yral.shared.features.auth.ui.LoginMode
import com.yral.shared.features.auth.ui.LoginScreenType
import com.yral.shared.features.auth.ui.rememberLoginInfo
import com.yral.shared.features.chat.attachments.FilePathChatAttachment
import com.yral.shared.features.chat.domain.models.ChatMessageType
import com.yral.shared.features.chat.domain.models.ConversationMessageRole
import com.yral.shared.features.chat.domain.models.SendMessageDraft
import com.yral.shared.features.chat.nav.conversation.ConversationComponent
import com.yral.shared.features.chat.viewmodel.ConversationMessageItem
import com.yral.shared.features.chat.viewmodel.ConversationViewModel
import com.yral.shared.libs.designsystem.component.YralLoader
import com.yral.shared.rust.service.utils.CanisterData
import com.yral.shared.rust.service.utils.getUserInfoServiceCanister
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import yral_mobile.shared.features.chat.generated.resources.Res
import yral_mobile.shared.features.chat.generated.resources.chat_background_inverted
import yral_mobile.shared.features.chat.generated.resources.no_conversation_id

/**
 * Chat screen for testing conversation functionality.
 *
 * - Creates conversation for a hardcoded influencer
 * - Shows messages (oldest at top / newest at bottom) using LazyColumn(reverseLayout = true)
 * - Supports sending TEXT and IMAGE messages and retrying failed messages
 */
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
fun ChatConversationScreen(
    modifier: Modifier = Modifier,
    component: ConversationComponent,
    viewModel: ConversationViewModel = koinViewModel(),
) {
    val viewState by viewModel.viewState.collectAsState()

    LaunchedEffect(component.influencerId, viewState.isSocialSignedIn) {
        viewModel.initializeForInfluencer(
            influencerId = component.influencerId,
            influencerCategory = component.influencerCategory,
            influencerSource = component.influencerSource,
        )
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

    // Auto-scroll to show first line of new assistant replies
    AutoScrollToAssistantMessage(
        readyForAutoScroll = readyForAutoScroll,
        latestAssistantMessage = latestAssistantState.value,
        targetIndex = latestAssistantState.index,
        listState = listState,
        screenWidth = screenWidth,
        density = density,
        overlayItems = overlayItems,
    )

    // Check if there's a waiting assistant message in overlay
    val hasWaitingAssistant by derivedStateOf {
        overlayItems.any { it.isWaitingAssistant() }
    }

    val overlayUserCount by derivedStateOf { overlayItems.count { item -> item.isUser() } }
    val historyUserCount by produceState(initialValue = 0, historyPagingItems) {
        snapshotFlow { historyPagingItems.itemSnapshotList.items }
            .map { items -> items.count { item -> item.isUser() } }
            .collect { value = it }
    }
    val shouldPromptForLogin by derivedStateOf {
        !viewState.isSocialSignedIn && (overlayUserCount + historyUserCount) >= LOGIN_PROMPT_MESSAGE_LIMIT
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

    fun sendMessageIfAllowed(
        draft: SendMessageDraft,
        onBeforeSend: () -> Unit = {},
    ) {
        if (shouldPromptForLogin) {
            promptLogin()
            return
        }
        onBeforeSend()
        viewModel.sendMessage(draft)
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .paint(
                    painter = painterResource(Res.drawable.chat_background_inverted),
                    contentScale = ContentScale.Crop,
                ),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            ChatHeader(
                influencer = viewState.influencer,
                onBackClick = { component.onBack() },
                onProfileClick = { influencer ->
                    val canisterData =
                        CanisterData(
                            canisterId = getUserInfoServiceCanister(),
                            userPrincipalId = influencer.id,
                            profilePic = influencer.avatarUrl,
                            username = influencer.name,
                            isCreatedFromServiceCanister = true,
                            isFollowing = false,
                        )
                    component.openProfile(canisterData)
                },
                onClearChat = { viewModel.deleteAndRecreateConversation(component.influencerId) },
                onShareProfile = { viewModel.shareProfile() },
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

                    viewState.error != null -> {
                        Text(
                            text = viewState.error.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    viewState.conversationId == null -> {
                        Text(
                            text = stringResource(Res.string.no_conversation_id),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    else -> {
                        val keyboardController = LocalSoftwareKeyboardController.current
                        MessagesList(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            listState = listState,
                            overlayItems = overlayItems,
                            historyPagingItems = historyPagingItems,
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
                            SuggestionMessagesRow(
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

                        // Input area with dropdown menu
                        ChatInputArea(
                            input = input,
                            onInputChange = { input = it },
                            onSendClick = {
                                keyboardController?.hide()
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
    }
}

private const val LOGIN_PROMPT_MESSAGE_LIMIT = 5
