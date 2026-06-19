package com.yral.shared.features.chat.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.yral.shared.core.session.SessionManager
import com.yral.shared.features.chat.domain.models.formatChatUnreadBadgeCount
import com.yral.shared.features.chat.nav.home.ChatHomeComponent
import com.yral.shared.features.chat.nav.home.ChatHomeComponent.Child
import com.yral.featureflag.ChatFeatureFlags
import com.yral.featureflag.FeatureFlagManager
import com.yral.shared.data.domain.models.ConversationInfluencerSource
import com.yral.shared.data.domain.models.OpenConversationParams
import com.yral.shared.features.chat.ui.inbox.InboxScreen
import com.yral.shared.features.chat.ui.wall.ChatWallScreen
import com.yral.shared.features.chat.ui.wall.DiscoverySearchBar
import com.yral.shared.features.chat.ui.wall.DiscoverySearchResults
import com.yral.shared.features.chat.ui.wall.InboxSearchResults
import com.yral.shared.features.chat.viewmodel.ChatWallViewModel
import com.yral.shared.features.chat.viewmodel.DiscoverySearchState
import com.yral.shared.features.chat.viewmodel.DiscoverySearchViewModel
import com.yral.shared.features.chat.viewmodel.InboxSearchState
import com.yral.shared.features.chat.viewmodel.InboxSearchViewModel
import com.yral.shared.features.chat.viewmodel.InboxViewModel
import com.yral.shared.libs.designsystem.component.CreateInfluencerButton
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import yral_mobile.shared.features.chat.generated.resources.Res
import yral_mobile.shared.features.chat.generated.resources.chat_wall_subtitle
import yral_mobile.shared.features.chat.generated.resources.chat_wall_title
import yral_mobile.shared.features.chat.generated.resources.discovery_search_placeholder
import yral_mobile.shared.features.chat.generated.resources.ic_tab_discover
import yral_mobile.shared.features.chat.generated.resources.ic_tab_inbox
import yral_mobile.shared.features.chat.generated.resources.inbox_search_placeholder
import yral_mobile.shared.features.chat.generated.resources.tab_discover
import yral_mobile.shared.features.chat.generated.resources.tab_inbox

@Suppress("MagicNumber")
private object ChatTabUi {
    const val TAB_WIDTH_FRACTION = 0.5f
    val SEPARATOR_HEIGHT = 3.dp
    val INDICATOR_RADIUS = 10.dp
    val TAB_VERTICAL_PADDING = 12.dp
    val TAB_ICON_SPACING = 6.dp
    val BADGE_MIN_SIZE = 18.dp
    val BADGE_HORIZONTAL_PADDING = 5.dp
    val BADGE_VERTICAL_PADDING = 1.dp
    val BADGE_Y_OFFSET = (-5).dp
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun ChatHomeScreen(
    component: ChatHomeComponent,
    modifier: Modifier = Modifier,
    sessionManager: SessionManager = koinInject(),
    chatWallViewModel: ChatWallViewModel = koinViewModel(),
    inboxViewModel: InboxViewModel = koinViewModel(),
    discoverySearchViewModel: DiscoverySearchViewModel = koinViewModel(),
    inboxSearchViewModel: InboxSearchViewModel = koinViewModel(),
    featureFlagManager: FeatureFlagManager = koinInject(),
) {
    val stack by component.stack.subscribeAsState()
    val activeChild = stack.active.instance
    val isDiscoverSelected = activeChild is Child.Discover
    val isBotAccount = sessionManager.isBotAccount == true

    val wallState by chatWallViewModel.state.collectAsStateWithLifecycle()
    val unreadConversationCount by inboxViewModel.unreadConversationCount.collectAsStateWithLifecycle()
    val showCreateBotCta by
        sessionManager
            .shouldShowCreateBotCtaFlow(wallState.maxBotCountForCta)
            .collectAsStateWithLifecycle(initialValue = false)

    val searchEnabled =
        remember(featureFlagManager) {
            featureFlagManager.isEnabled(ChatFeatureFlags.Chat.DiscoverySearchEnabled)
        }
    val discoveryState by discoverySearchViewModel.state.collectAsStateWithLifecycle()
    val inboxState by inboxSearchViewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.Black),
    ) {
        ChatHomeHeaderBlock(
            isBotAccount = isBotAccount,
            searchEnabled = searchEnabled,
            isDiscoverSelected = isDiscoverSelected,
            showCreateBotCta = showCreateBotCta,
            unreadConversationCount = unreadConversationCount,
            discoverySearchViewModel = discoverySearchViewModel,
            inboxSearchViewModel = inboxSearchViewModel,
            onCreateInfluencerClick = {
                chatWallViewModel.trackCreateInfluencerClicked()
                component.openCreateInfluencer()
            },
            onDiscoverTabClick = component::onDiscoverTabClick,
            onInboxTabClick = component::onInboxTabClick,
        )
        Box(modifier = Modifier.fillMaxSize()) {
            ChatHomeContent(
                activeChild = activeChild,
                searchEnabled = searchEnabled,
                discoveryState = discoveryState,
                inboxState = inboxState,
                chatWallViewModel = chatWallViewModel,
                inboxViewModel = inboxViewModel,
            )
        }
    }
}

/**
 * Header swap: bot accounts see only the legacy InboxTitle (no tabs,
 * no search). Everyone else sees either the symmetric search bar or
 * the legacy "Chat / Create Influencer" row, gated by [searchEnabled],
 * with the Discover / Inbox tab row underneath.
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
private fun ChatHomeHeaderBlock(
    isBotAccount: Boolean,
    searchEnabled: Boolean,
    isDiscoverSelected: Boolean,
    showCreateBotCta: Boolean,
    unreadConversationCount: Int,
    discoverySearchViewModel: DiscoverySearchViewModel,
    inboxSearchViewModel: InboxSearchViewModel,
    onCreateInfluencerClick: () -> Unit,
    onDiscoverTabClick: () -> Unit,
    onInboxTabClick: () -> Unit,
) {
    if (isBotAccount) {
        InboxTitle()
        return
    }
    if (searchEnabled) {
        ChatHomeSearchHeader(
            isDiscoverSelected = isDiscoverSelected,
            discoverySearchViewModel = discoverySearchViewModel,
            inboxSearchViewModel = inboxSearchViewModel,
            onCreateInfluencerClick = onCreateInfluencerClick,
        )
    } else {
        ChatHomeHeader(
            showCreateBotCta = showCreateBotCta,
            onCreateInfluencerClick = onCreateInfluencerClick,
        )
    }
    ChatTabRow(
        isDiscoverSelected = isDiscoverSelected,
        inboxUnreadCount = unreadConversationCount,
        onDiscoverClick = onDiscoverTabClick,
        onInboxClick = onInboxTabClick,
    )
}

/**
 * Symmetric search bar above the tab row. Routes the typed query to
 * whichever VM matches the active tab; the OTHER tab's VM keeps its
 * own state so a half-formed query is preserved across a brief tab
 * detour. Each VM owns its own LRU cache + debounce window.
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
private fun ChatHomeSearchHeader(
    isDiscoverSelected: Boolean,
    discoverySearchViewModel: DiscoverySearchViewModel,
    inboxSearchViewModel: InboxSearchViewModel,
    onCreateInfluencerClick: () -> Unit,
) {
    val discoveryQuery by discoverySearchViewModel.query.collectAsStateWithLifecycle()
    val inboxQuery by inboxSearchViewModel.query.collectAsStateWithLifecycle()
    val activeQuery = if (isDiscoverSelected) discoveryQuery else inboxQuery
    val activePlaceholder =
        stringResource(
            if (isDiscoverSelected) Res.string.discovery_search_placeholder
            else Res.string.inbox_search_placeholder,
        )
    val onActiveQueryChange: (String) -> Unit =
        remember(isDiscoverSelected) {
            if (isDiscoverSelected) discoverySearchViewModel::setQuery
            else inboxSearchViewModel::setQuery
        }
    val onActiveClear: () -> Unit =
        remember(isDiscoverSelected) {
            if (isDiscoverSelected) discoverySearchViewModel::clearQuery
            else inboxSearchViewModel::clearQuery
        }
    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
        DiscoverySearchBar(
            query = activeQuery,
            placeholder = activePlaceholder,
            onQueryChange = onActiveQueryChange,
            onCreateClick = onCreateInfluencerClick,
            onClearClick = onActiveClear,
        )
    }
}

/**
 * Active-child content swap: Discover grid + search overlay vs.
 * Inbox screen + search overlay. Each branch hands its child
 * component to the corresponding feature screen and overlays the
 * search results when the matching VM has an active query.
 */
@Composable
private fun ChatHomeContent(
    activeChild: Child,
    searchEnabled: Boolean,
    discoveryState: DiscoverySearchState,
    inboxState: InboxSearchState,
    chatWallViewModel: ChatWallViewModel,
    inboxViewModel: InboxViewModel,
) {
    when (activeChild) {
        is Child.Discover -> {
            if (searchEnabled && discoveryState.isActive) {
                DiscoverySearchResults(
                    query = discoveryState.query,
                    results = discoveryState.results,
                    isLoading = discoveryState.isLoading,
                    error = discoveryState.error,
                    onResultClick = { result ->
                        activeChild.component.openConversation(
                            OpenConversationParams(
                                influencerId = result.id,
                                influencerCategory = result.category.orEmpty(),
                                influencerSource = ConversationInfluencerSource.CARD,
                                displayName = result.displayName,
                                username = result.name,
                                avatarUrl = result.avatarUrl,
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                ChatWallScreen(
                    component = activeChild.component,
                    viewModel = chatWallViewModel,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        is Child.Inbox -> {
            if (searchEnabled && inboxState.isActive) {
                InboxSearchResults(
                    query = inboxState.query,
                    results = inboxState.results,
                    isLoading = inboxState.isLoading,
                    error = inboxState.error,
                    onResultClick = { result ->
                        activeChild.component.openConversation(
                            OpenConversationParams(
                                conversationId = result.conversationId,
                                influencerId = result.influencerId,
                                influencerCategory = result.category.orEmpty(),
                                influencerSource = ConversationInfluencerSource.CARD,
                                displayName = result.displayName,
                                username = result.name,
                                avatarUrl = result.avatarUrl,
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                InboxScreen(
                    component = activeChild.component,
                    viewModel = inboxViewModel,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun InboxTitle() {
    Text(
        text = stringResource(Res.string.tab_inbox),
        style = LocalAppTopography.current.xlBold,
        color = YralColors.Grey50,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 16.dp),
    )
}

@Suppress("MagicNumber")
@OptIn(ExperimentalResourceApi::class)
@Composable
private fun ChatHomeHeader(
    showCreateBotCta: Boolean,
    onCreateInfluencerClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.chat_wall_title),
                style = LocalAppTopography.current.xlBold,
                color = YralColors.Grey50,
                modifier = Modifier.weight(1f),
            )
            if (showCreateBotCta) {
                CreateInfluencerButton(
                    modifier = Modifier.height(32.dp),
                    alignIconToEnd = false,
                    onClick = onCreateInfluencerClick,
                )
            }
        }
        Text(
            text = stringResource(Res.string.chat_wall_subtitle),
            style = LocalAppTopography.current.baseRegular,
            color = YralColors.Grey0,
            modifier = Modifier.padding(bottom = 20.dp),
        )
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun ChatTabRow(
    isDiscoverSelected: Boolean,
    inboxUnreadCount: Int,
    onDiscoverClick: () -> Unit,
    onInboxClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            ChatTabItem(
                modifier = Modifier.align(Alignment.CenterStart),
                iconRes = Res.drawable.ic_tab_discover,
                label = stringResource(Res.string.tab_discover),
                isSelected = isDiscoverSelected,
                onClick = onDiscoverClick,
            )
            ChatTabItem(
                modifier = Modifier.align(Alignment.CenterEnd),
                iconRes = Res.drawable.ic_tab_inbox,
                label = stringResource(Res.string.tab_inbox),
                isSelected = !isDiscoverSelected,
                badgeText = formatChatUnreadBadgeCount(inboxUnreadCount),
                onClick = onInboxClick,
            )
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(ChatTabUi.SEPARATOR_HEIGHT)
                    .background(YralColors.Neutral700),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(ChatTabUi.TAB_WIDTH_FRACTION)
                        .height(ChatTabUi.SEPARATOR_HEIGHT)
                        .clip(RoundedCornerShape(ChatTabUi.INDICATOR_RADIUS))
                        .align(
                            if (isDiscoverSelected) {
                                Alignment.CenterStart
                            } else {
                                Alignment.CenterEnd
                            },
                        ).background(YralColors.Pink300),
            )
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun ChatTabItem(
    modifier: Modifier = Modifier,
    iconRes: DrawableResource,
    label: String,
    isSelected: Boolean,
    badgeText: String? = null,
    onClick: () -> Unit,
) {
    val contentColor =
        if (isSelected) YralColors.NeutralTextPrimary else YralColors.NeutralTextSecondary

    Box(
        modifier =
            Modifier
                .fillMaxWidth(ChatTabUi.TAB_WIDTH_FRACTION)
                .then(modifier)
                .clickable(onClick = onClick)
                .padding(vertical = ChatTabUi.TAB_VERTICAL_PADDING),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ChatTabUi.TAB_ICON_SPACING),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = label,
                tint = contentColor,
            )
            Text(
                text = label,
                style = LocalAppTopography.current.baseMedium,
                color = contentColor,
            )
            if (badgeText != null) {
                ChatTabBadge(
                    text = badgeText,
                    modifier = Modifier.offset(y = ChatTabUi.BADGE_Y_OFFSET),
                )
            }
        }
    }
}

@Composable
private fun ChatTabBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .defaultMinSize(
                    minWidth = ChatTabUi.BADGE_MIN_SIZE,
                    minHeight = ChatTabUi.BADGE_MIN_SIZE,
                ).background(
                    color = YralColors.Pink300,
                    shape = RoundedCornerShape(100.dp),
                ).padding(
                    horizontal = ChatTabUi.BADGE_HORIZONTAL_PADDING,
                    vertical = ChatTabUi.BADGE_VERTICAL_PADDING,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = LocalAppTopography.current.smSemiBold,
            color = Color.White,
        )
    }
}
