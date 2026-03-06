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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yral.shared.features.feed.nav.FeedComponent
import com.yral.shared.features.feed.ui.FeedActionsRight
import com.yral.shared.features.feed.ui.FeedScreen
import com.yral.shared.features.feed.ui.components.FeedOnboardingNudge
import com.yral.shared.features.feed.ui.components.FeedTargetBounds
import com.yral.shared.features.feed.ui.components.UserBrief
import com.yral.shared.features.feed.viewmodel.FeedState
import com.yral.shared.features.feed.viewmodel.FeedTab
import com.yral.shared.features.feed.viewmodel.FeedViewModel
import com.yral.shared.features.feed.viewmodel.OnboardingStep
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.rust.service.domain.models.toCanisterData
import com.yral.shared.rust.service.utils.CanisterData
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.app.generated.resources.Res
import yral_mobile.shared.app.generated.resources.feed_btn_chat
import yral_mobile.shared.app.generated.resources.feed_btn_skip
import yral_mobile.shared.app.generated.resources.feed_btn_yral
import yral_mobile.shared.app.generated.resources.feed_tab_explore
import yral_mobile.shared.app.generated.resources.feed_tab_influencers
import yral_mobile.shared.app.generated.resources.onboarding_nudge_balance
import yral_mobile.shared.app.generated.resources.onboarding_nudge_balance_highlight
import yral_mobile.shared.app.generated.resources.onboarding_nudge_rank
import yral_mobile.shared.app.generated.resources.onboarding_nudge_rank_highlight
import yral_mobile.shared.libs.designsystem.generated.resources.shadow
import com.yral.shared.features.feed.ui.components.ArrowAlignment as FeedArrowAlignment
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

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
        topOverlay = { pageNo ->
            Column {
                FeedTabBar(
                    selectedTab = selectedTab,
                    onTabSelected = { feedViewModel.setSelectedFeedTab(it) },
                )
                OverLayTop(
                    pageNo = pageNo,
                    feedState = feedState,
                    setPostDescriptionExpanded = { feedViewModel.setPostDescriptionExpanded(it) },
                    openUserProfile = { component.openProfile(it) },
                    feedViewModel = feedViewModel,
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
private fun OverLayTop(
    pageNo: Int,
    feedState: FeedState,
    setPostDescriptionExpanded: (Boolean) -> Unit,
    openUserProfile: (canisterData: CanisterData) -> Unit,
    feedViewModel: FeedViewModel,
) {
    val targetBounds by remember { mutableStateOf<FeedTargetBounds?>(null) }
    Box(modifier = Modifier.fillMaxSize()) {
        OverlayTopDefault(
            pageNo = pageNo,
            feedState = feedState,
            setPostDescriptionExpanded = setPostDescriptionExpanded,
            openUserProfile = openUserProfile,
        )
        if (feedState.currentPageOfFeed >= 0) {
            when (feedState.currentOnboardingStep) {
                OnboardingStep.INTRO_RANK -> {
                    FeedOnboardingNudge(
                        text = stringResource(Res.string.onboarding_nudge_rank),
                        highlightText = stringResource(Res.string.onboarding_nudge_rank_highlight),
                        arrowAlignment = FeedArrowAlignment.TOP_START,
                        isDismissible = false,
                        targetBounds = targetBounds,
                        onDismiss = { feedViewModel.dismissOnboardingStep() },
                    )
                }

                OnboardingStep.INTRO_BALANCE -> {
                    FeedOnboardingNudge(
                        text = stringResource(Res.string.onboarding_nudge_balance),
                        highlightText = stringResource(Res.string.onboarding_nudge_balance_highlight),
                        arrowAlignment = FeedArrowAlignment.TOP_END,
                        isDismissible = feedState.isMandatoryLogin,
                        targetBounds = targetBounds,
                        onDismiss = { feedViewModel.dismissOnboardingStep() },
                        isShowNext = !feedState.isMandatoryLogin,
                    )
                }

                else -> {}
            }
        }
    }
}

@Composable
private fun OverlayTopDefault(
    pageNo: Int,
    feedState: FeedState,
    setPostDescriptionExpanded: (Boolean) -> Unit,
    openUserProfile: (canisterData: CanisterData) -> Unit,
) {
    val feedDetails = feedState.feedDetails[pageNo]
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .paint(
                    painter = painterResource(DesignRes.drawable.shadow),
                    contentScale = ContentScale.FillBounds,
                ).padding(end = 26.dp),
    ) {
        UserBrief(
            principalId = feedDetails.principalID,
            profileImageUrl = feedDetails.profileImageURL,
            displayName = feedDetails.displayName.ifBlank { null },
            postDescription = feedDetails.postDescription,
            isPostDescriptionExpanded = feedState.isPostDescriptionExpanded,
            setPostDescriptionExpanded = { setPostDescriptionExpanded(it) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(end = 46.dp)
                    .clickable { openUserProfile(feedDetails.toCanisterData()) },
        )
    }
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
