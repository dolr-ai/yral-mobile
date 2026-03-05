package com.yral.shared.features.chat.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.yral.shared.core.session.SessionManager
import com.yral.shared.features.chat.nav.home.ChatHomeComponent
import com.yral.shared.features.chat.nav.home.ChatHomeComponent.Child
import com.yral.shared.features.chat.ui.inbox.InboxScreen
import com.yral.shared.features.chat.ui.wall.ChatWallScreen
import com.yral.shared.features.chat.viewmodel.ChatWallViewModel
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
import yral_mobile.shared.features.chat.generated.resources.ic_tab_discover
import yral_mobile.shared.features.chat.generated.resources.ic_tab_inbox
import yral_mobile.shared.features.chat.generated.resources.tab_discover
import yral_mobile.shared.features.chat.generated.resources.tab_inbox

@Suppress("MagicNumber")
private object ChatTabUi {
    const val TAB_WIDTH_FRACTION = 0.5f
    val SEPARATOR_HEIGHT = 3.dp
    val INDICATOR_RADIUS = 10.dp
    val TAB_VERTICAL_PADDING = 12.dp
    val TAB_ICON_SPACING = 6.dp
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun ChatHomeScreen(
    component: ChatHomeComponent,
    modifier: Modifier = Modifier,
    sessionManager: SessionManager = koinInject(),
    chatWallViewModel: ChatWallViewModel = koinViewModel(),
    inboxViewModel: InboxViewModel = koinViewModel(),
) {
    val stack by component.stack.subscribeAsState()
    val activeChild = stack.active.instance
    val isDiscoverSelected = activeChild is Child.Discover
    val isBotAccount = sessionManager.isBotAccount == true

    val wallState by chatWallViewModel.state.collectAsStateWithLifecycle()
    val showCreateBotCta by
        sessionManager
            .shouldShowCreateBotCtaFlow(wallState.maxBotCountForCta)
            .collectAsStateWithLifecycle(initialValue = false)

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.Black),
    ) {
        if (isBotAccount) {
            InboxTitle()
        } else {
            ChatHomeHeader(
                showCreateBotCta = showCreateBotCta,
                onCreateInfluencerClick = {
                    chatWallViewModel.trackCreateInfluencerClicked()
                    component.openCreateInfluencer()
                },
            )
            ChatTabRow(
                isDiscoverSelected = isDiscoverSelected,
                onDiscoverClick = component::onDiscoverTabClick,
                onInboxClick = component::onInboxTabClick,
            )
        }
        Box(modifier = Modifier.fillMaxSize()) {
            when (activeChild) {
                is Child.Discover ->
                    ChatWallScreen(
                        component = activeChild.component,
                        viewModel = chatWallViewModel,
                        modifier = Modifier.fillMaxSize(),
                    )
                is Child.Inbox ->
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
        }
    }
}
