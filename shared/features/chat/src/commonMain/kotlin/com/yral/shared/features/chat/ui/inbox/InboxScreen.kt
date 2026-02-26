package com.yral.shared.features.chat.ui.inbox

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.yral.shared.core.utils.resolveUsername
import com.yral.shared.data.domain.models.OpenConversationParams
import com.yral.shared.features.chat.domain.models.Conversation
import com.yral.shared.features.chat.nav.inbox.InboxComponent
import com.yral.shared.features.chat.viewmodel.InboxViewModel
import com.yral.shared.libs.designsystem.component.YralLoader
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.rust.service.domain.models.UserProfileDetails
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.features.chat.generated.resources.Res
import yral_mobile.shared.features.chat.generated.resources.error_load_conversations
import yral_mobile.shared.features.chat.generated.resources.no_conversation_yet

@Composable
fun InboxScreen(
    component: InboxComponent,
    viewModel: InboxViewModel,
    modifier: Modifier = Modifier,
) {
    val pagingItems = viewModel.conversations.collectAsLazyPagingItems()
    val inboxState by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshConversations()
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (pagingItems.loadState.refresh) {
            is LoadState.Loading ->
                if (pagingItems.itemCount == 0) {
                    InboxLoadingState()
                } else {
                    InboxConversationList(
                        pagingItems = pagingItems,
                        isBotAccount = inboxState.isBotAccount,
                        profileDetailsByUserId = inboxState.profileDetailsByUserId,
                        contentOffsetY = 0.dp,
                        onConversationClick = { conversation ->
                            component.openConversation(
                                OpenConversationParams(
                                    influencerId = conversation.influencer.id,
                                    influencerCategory = conversation.influencer.category,
                                    conversationId = conversation.id,
                                    userId = conversation.userId,
                                ),
                            )
                        },
                    )
                }
            is LoadState.Error -> InboxErrorState()
            is LoadState.NotLoading ->
                if (pagingItems.itemCount == 0) {
                    InboxEmptyState()
                } else {
                    InboxConversationList(
                        pagingItems = pagingItems,
                        isBotAccount = inboxState.isBotAccount,
                        profileDetailsByUserId = inboxState.profileDetailsByUserId,
                        contentOffsetY = 0.dp,
                        onConversationClick = { conversation ->
                            component.openConversation(
                                OpenConversationParams(
                                    influencerId = conversation.influencer.id,
                                    influencerCategory = conversation.influencer.category,
                                    conversationId = conversation.id,
                                    userId = conversation.userId,
                                ),
                            )
                        },
                    )
                }
        }
    }
}

@Composable
private fun InboxLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        YralLoader()
    }
}

@Composable
private fun InboxErrorState() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(Res.string.error_load_conversations),
            style = LocalAppTopography.current.baseRegular,
            color = YralColors.NeutralTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
    }
}

@Composable
private fun InboxEmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(Res.string.no_conversation_yet),
            style = LocalAppTopography.current.baseRegular,
            color = YralColors.NeutralTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
    }
}

@Composable
private fun InboxConversationList(
    pagingItems: LazyPagingItems<Conversation>,
    isBotAccount: Boolean,
    profileDetailsByUserId: Map<String, UserProfileDetails>,
    contentOffsetY: androidx.compose.ui.unit.Dp = 0.dp,
    onConversationClick: (Conversation) -> Unit,
) {
    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .offset(y = contentOffsetY),
    ) {
        items(
            count = pagingItems.itemCount,
            key = pagingItems.itemKey { it.id },
            contentType = pagingItems.itemContentType { "Conversation" },
        ) { index ->
            val conversation = pagingItems[index] ?: return@items
            val profileDetails = if (isBotAccount) profileDetailsByUserId[conversation.userId] else null
            ConversationListItem(
                conversation = conversation,
                overrideAvatarUrl = profileDetails?.profilePictureUrl,
                overrideDisplayName = resolveUsername(null, profileDetails?.principalId),
                onClick = { onConversationClick(conversation) },
            )
        }
    }
}
