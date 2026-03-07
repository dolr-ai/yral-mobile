package com.yral.shared.app.ui.screens.feed

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yral.shared.features.feed.nav.FeedComponent
import com.yral.shared.features.feed.ui.FeedActionsRight
import com.yral.shared.features.feed.ui.FeedScreen
import com.yral.shared.features.feed.viewmodel.FeedTab
import com.yral.shared.features.feed.viewmodel.FeedViewModel
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.app.generated.resources.Res
import yral_mobile.shared.app.generated.resources.feed_btn_chat
import yral_mobile.shared.app.generated.resources.feed_btn_skip
import yral_mobile.shared.app.generated.resources.feed_btn_yral
import yral_mobile.shared.app.generated.resources.feed_tab_explore
import yral_mobile.shared.app.generated.resources.feed_tab_influencers

@Composable
fun FeedScaffoldScreen(
    component: FeedComponent,
    feedViewModel: FeedViewModel,
    onNavigateToChat: () -> Unit,
) {
    val feedState by feedViewModel.state.collectAsStateWithLifecycle()
    val selectedTab = feedState.selectedFeedTab
    FeedScreen(
        component = component,
        viewModel = feedViewModel,
        topOverlay = { _ ->
            Column {
                FeedTabBar(
                    selectedTab = selectedTab,
                    onTabSelected = { feedViewModel.setSelectedFeedTab(it) },
                )
            }
        },
        bottomOverlay = { _, scrollToNext ->
            Box(modifier = Modifier.fillMaxSize()) {
                FeedActionButtons(
                    selectedTab = selectedTab,
                    onSkip = scrollToNext,
                    onNavigateToChat = onNavigateToChat,
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp),
                )
            }
        },
        actionsRight = { pageNo ->
            FeedActionsRight(pageNo, feedState, feedViewModel, component::openProfile)
        },
        onPageChanged = { _, _ -> },
        onEdgeScrollAttempt = { _ -> },
        limitReelCount = feedState.feedDetails.size,
    )
}

@Composable
private fun FeedTabBar(
    selectedTab: FeedTab,
    onTabSelected: (FeedTab) -> Unit,
) {
    val typography = LocalAppTopography.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        FeedTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            val label =
                when (tab) {
                    FeedTab.EXPLORE -> stringResource(Res.string.feed_tab_explore)
                    FeedTab.INFLUENCERS -> stringResource(Res.string.feed_tab_influencers)
                }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier =
                    Modifier
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = label,
                    style = typography.mdSemiBold,
                    color = Color.White,
                    modifier =
                        if (!isSelected) {
                            Modifier.alpha(FeedScaffoldScreenConstants.UNSELECTED_TAB_ALPHA)
                        } else {
                            Modifier
                        },
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier =
                        Modifier
                            .width(29.dp)
                            .height(4.dp)
                            .background(
                                color = if (isSelected) YralColors.Pink200 else Color.Transparent,
                                shape = CircleShape,
                            ),
                )
            }
        }
    }
}

@Composable
private fun FeedActionButtons(
    selectedTab: FeedTab,
    onSkip: () -> Unit,
    onNavigateToChat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(80.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(Res.drawable.feed_btn_skip),
            contentDescription = "Skip",
            modifier =
                Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .clickable { onSkip() },
        )
        when (selectedTab) {
            FeedTab.EXPLORE -> {
                Image(
                    painter = painterResource(Res.drawable.feed_btn_yral),
                    contentDescription = "Yral",
                    modifier =
                        Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .clickable { onSkip() },
                )
            }
            FeedTab.INFLUENCERS -> {
                Image(
                    painter = painterResource(Res.drawable.feed_btn_chat),
                    contentDescription = "Chat",
                    modifier =
                        Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .clickable { onNavigateToChat() },
                )
            }
        }
    }
}

private object FeedScaffoldScreenConstants {
    const val UNSELECTED_TAB_ALPHA = 0.6f
}
