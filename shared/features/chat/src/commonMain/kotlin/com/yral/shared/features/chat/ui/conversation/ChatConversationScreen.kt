package com.yral.shared.features.chat.ui.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import com.yral.shared.features.chat.attachments.FilePathChatAttachment
import com.yral.shared.features.chat.domain.models.ChatMessageType
import com.yral.shared.features.chat.domain.models.SendMessageDraft
import com.yral.shared.features.chat.nav.conversation.ConversationComponent
import com.yral.shared.features.chat.viewmodel.ConversationViewModel
import com.yral.shared.libs.designsystem.component.YralLoader
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.rust.service.utils.CanisterData
import com.yral.shared.rust.service.utils.getUserInfoServiceCanister
import org.koin.compose.viewmodel.koinViewModel

/**
 * Chat screen for testing conversation functionality.
 *
 * - Creates conversation for a hardcoded influencer
 * - Shows messages (oldest at top / newest at bottom) using LazyColumn(reverseLayout = true)
 * - Supports sending TEXT and IMAGE messages and retrying failed messages
 */
@Suppress("LongMethod")
@Composable
fun ChatConversationScreen(
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp,
    component: ConversationComponent,
    viewModel: ConversationViewModel = koinViewModel(),
) {
    LaunchedEffect(component.influencerId) {
        viewModel.initializeForInfluencer(component.influencerId)
    }
    val viewState by viewModel.viewState.collectAsState()

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

    val imeBottomDp = with(density) { WindowInsets.ime.getBottom(this).toDp() }
    val keyboardAwareBottomPadding = (imeBottomDp - bottomPadding).coerceAtLeast(0.dp)

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(YralColors.PrimaryContainer),
        ) {
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
                onShareProfile = { /* No op */ },
            )

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                        .padding(bottom = keyboardAwareBottomPadding),
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
                        Text(text = "No conversation id", color = MaterialTheme.colorScheme.error)
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

                        // Input area with dropdown menu
                        ChatInputArea(
                            input = input,
                            onInputChange = { input = it },
                            onSendClick = {
                                keyboardController?.hide()
                                val text = input.trim()
                                input = ""
                                viewModel.sendMessage(
                                    SendMessageDraft(
                                        messageType = ChatMessageType.TEXT,
                                        content = text,
                                    ),
                                )
                            },
                            onCameraClick = imageCaptureLauncher,
                            onGalleryClick = imagePickerLauncher,
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
                    viewModel.sendMessage(draft)
                    selectedImage = null
                },
                onDismiss = { selectedImage = null },
                bottomPadding = bottomPadding,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
