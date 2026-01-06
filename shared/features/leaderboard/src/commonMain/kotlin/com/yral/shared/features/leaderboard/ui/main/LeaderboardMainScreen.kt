package com.yral.shared.features.leaderboard.ui.main

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.yral.shared.features.leaderboard.data.models.LeaderboardMode
import com.yral.shared.features.leaderboard.domain.models.LeaderboardItem
import com.yral.shared.features.leaderboard.domain.models.RewardCurrency
import com.yral.shared.features.leaderboard.nav.main.LeaderboardMainComponent
import com.yral.shared.features.leaderboard.viewmodel.LeaderBoardState
import com.yral.shared.features.leaderboard.viewmodel.LeaderBoardViewModel
import com.yral.shared.libs.designsystem.component.YralButtonType
import com.yral.shared.libs.designsystem.component.YralGradientButton
import com.yral.shared.libs.designsystem.component.YralLoader
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.libs.leaderboard.model.LeaderboardEntry
import com.yral.shared.libs.leaderboard.ui.LeaderboardConfetti
import com.yral.shared.libs.leaderboard.ui.LeaderboardRow
import com.yral.shared.libs.leaderboard.ui.LeaderboardTableHeader
import com.yral.shared.libs.leaderboard.ui.main.LeaderboardUiConstants
import com.yral.shared.libs.leaderboard.ui.main.TrophyGallery
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import yral_mobile.shared.libs.designsystem.generated.resources.arrow_left
import yral_mobile.shared.libs.leaderboard.generated.resources.Res
import yral_mobile.shared.libs.leaderboard.generated.resources.leaderboard
import yral_mobile.shared.libs.leaderboard.generated.resources.play_games_to_claim_your_spot
import yral_mobile.shared.libs.leaderboard.generated.resources.purple_leaderboard
import yral_mobile.shared.libs.leaderboard.generated.resources.start_playing
import yral_mobile.shared.libs.leaderboard.generated.resources.yellow_leaderboard
import kotlin.math.max
import kotlin.math.min
import com.yral.shared.libs.leaderboard.model.LeaderboardMode as SharedLeaderboardMode
import com.yral.shared.libs.leaderboard.model.RewardCurrency as SharedRewardCurrency
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@Suppress("LongMethod", "UnusedParameter", "CyclomaticComplexMethod")
@Composable
fun LeaderboardMainScreen(
    component: LeaderboardMainComponent,
    modifier: Modifier = Modifier,
    viewModel: LeaderBoardViewModel = koinViewModel(),
) {
    val countryCode = Locale.current.region
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.leaderboardPageViewed() }

    LaunchedEffect(state.navigationEvent) {
        state.navigationEvent?.let { canisterData ->
            component.openProfile(canisterData)
            viewModel.onNavigationHandled()
        }
    }

    var showConfetti by remember(state.isCurrentUserInTop) { mutableStateOf(state.isCurrentUserInTop) }

    LaunchedEffect(state.isFirebaseLoggedIn) {
        if (state.isFirebaseLoggedIn) {
            viewModel.loadData(countryCode)
        }
    }
    val isEmptyStateVisible by remember(state.isLoading, state.isFirebaseLoggedIn, state.leaderboard) {
        derivedStateOf {
            !state.isLoading &&
                state.isFirebaseLoggedIn &&
                state.leaderboard.isEmpty()
        }
    }

    val listState = rememberLazyListState()
    var isTrophyVisible by remember { mutableStateOf(true) }
    var pageLoadedReported by remember(state.selectedMode) { mutableStateOf(false) }
    LaunchedEffect(state.isLoading) {
        if (state.isLoading) pageLoadedReported = false
    }
    LaunchedEffect(listState, state.isLoading, state.error, state.selectedMode) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .collect { itemsInfo ->
                if (!state.isLoading && state.error == null && !pageLoadedReported) {
                    val viewportStart = listState.layoutInfo.viewportStartOffset
                    val viewportEnd = listState.layoutInfo.viewportEndOffset
                    val visibleRowCount =
                        itemsInfo.count { info ->
                            if (info.contentType != "leaderboardRow") return@count false
                            val itemStart = info.offset
                            val itemEnd = info.offset + info.size
                            val visible = min(itemEnd, viewportEnd) - max(itemStart, viewportStart)
                            // Include only if strictly more than 50% visible
                            visible > 0 && visible * 2 > info.size
                        }
                    if (visibleRowCount > 0) {
                        viewModel.reportLeaderboardPageLoaded(visibleRowCount)
                        pageLoadedReported = true
                    }
                }
            }
    }
    val leaderboardBG =
        when (state.selectedMode) {
            LeaderboardMode.DAILY -> Res.drawable.yellow_leaderboard
            LeaderboardMode.ALL_TIME -> Res.drawable.purple_leaderboard
        }
    val sharedRewardCurrency = state.rewardCurrency?.toSharedRewardCurrency()
    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier =
                Modifier
                    .fillMaxSize()
                    .nestedScroll(
                        object : NestedScrollConnection {
                            override suspend fun onPostFling(
                                consumed: Velocity,
                                available: Velocity,
                            ): Velocity {
                                isTrophyVisible = consumed.y > 0
                                return super.onPostFling(consumed, available)
                            }
                        },
                    ),
        ) {
            stickyHeader {
                LeaderboardHeader(
                    countryCode = countryCode,
                    state = state,
                    isFirebaseLoggedIn = state.isFirebaseLoggedIn,
                    component = component,
                    isTrophyVisible = isTrophyVisible || state.isLoading,
                    viewModel = viewModel,
                    leaderboardBG = leaderboardBG,
                    trackOpenHistory = { viewModel.leaderboardCalendarClicked() },
                    showBack = component.showBackIcon,
                    onBack = component.onBack,
                )
            }
            if (!state.isLoading && state.error == null) {
                // Show current user first if available
                state.currentUser?.let { user ->
                    item(contentType = "leaderboardRow") {
                        Column(
                            modifier =
                                Modifier
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                        ) {
                            LeaderboardRow(
                                position = user.position,
                                userIdentifier = user.username,
                                profileImageUrl = user.profileImage,
                                wins = user.wins,
                                isCurrentUser = true,
                                decorateCurrentUser = true,
                                rewardCurrency = sharedRewardCurrency,
                                rewardCurrencyCode = state.rewardCurrencyCode,
                                reward = user.reward,
                                onClick = { viewModel.onUserClick(user) },
                            )
                        }
                    }
                }

                // Leaderboard items
                items(items = state.leaderboard, contentType = { "leaderboardRow" }) { item ->
                    Column(
                        modifier =
                            Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(start = 16.dp, end = 16.dp, top = 8.dp),
                    ) {
                        LeaderboardRow(
                            position = item.position,
                            userIdentifier = item.username,
                            profileImageUrl = item.profileImage,
                            wins = item.wins,
                            isCurrentUser = viewModel.isCurrentUser(item.userPrincipalId),
                            decorateCurrentUser = false,
                            rewardCurrency = sharedRewardCurrency,
                            rewardCurrencyCode = state.rewardCurrencyCode,
                            reward = item.reward,
                            onClick = { viewModel.onUserClick(item) },
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                if (isEmptyStateVisible) {
                    item { EmptyState(isTrophyVisible, component) }
                }

                // Bottom padding
                item { Spacer(modifier = Modifier.height(68.dp)) }
            }
            if (!state.isLoading && state.error != null) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = state.error ?: "",
                            style = LocalAppTopography.current.baseMedium,
                            color = YralColors.Neutral500,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
        LeaderboardConfetti(showConfetti) { showConfetti = false }

        if (state.isNavigating) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                YralLoader()
            }
        }
    }
}

@Composable
private fun EmptyState(
    isTrophyVisible: Boolean,
    component: LeaderboardMainComponent,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(30.dp, Alignment.Top),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
                Modifier
                    .width(270.dp)
                    .padding(top = if (!isTrophyVisible) 210.dp else 68.dp),
        ) {
            Text(
                text = stringResource(Res.string.play_games_to_claim_your_spot),
                style = LocalAppTopography.current.lgMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            YralGradientButton(
                text = stringResource(Res.string.start_playing),
                buttonType = YralButtonType.White,
                onClick = { component.navigateToHome() },
            )
        }
    }
}

@Composable
private fun LeaderboardTitle(onBack: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(DesignRes.drawable.arrow_left),
            contentDescription = "back",
            tint = YralColors.Neutral950,
            modifier =
                Modifier
                    .size(24.dp)
                    .clickable { onBack() },
        )
        Text(
            text = stringResource(Res.string.leaderboard),
            style = LocalAppTopography.current.xlBold,
            color = YralColors.Neutral950,
            modifier = Modifier.weight(1f).offset(x = (-12).dp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LeaderboardHeader(
    countryCode: String,
    state: LeaderBoardState,
    isFirebaseLoggedIn: Boolean,
    component: LeaderboardMainComponent,
    isTrophyVisible: Boolean,
    viewModel: LeaderBoardViewModel,
    leaderboardBG: DrawableResource,
    trackOpenHistory: () -> Unit,
    showBack: Boolean,
    onBack: () -> Unit,
) {
    val brushColors =
        when (state.selectedMode) {
            LeaderboardMode.DAILY -> LeaderboardUiConstants.YELLOW_BRUSH
            LeaderboardMode.ALL_TIME -> LeaderboardUiConstants.PURPLE_BRUSH
        }
    val sharedMode = state.selectedMode.toSharedMode()
    val sharedRewardCurrency = state.rewardCurrency?.toSharedRewardCurrency()
    val loading by remember(state.isLoading, isFirebaseLoggedIn) {
        mutableStateOf(state.isLoading || !isFirebaseLoggedIn)
    }
    Box {
        Image(
            painter = painterResource(leaderboardBG),
            contentDescription = "",
            contentScale = ContentScale.Crop,
            alignment = if (isTrophyVisible) Alignment.Center else Alignment.TopCenter,
            modifier = Modifier.matchParentSize(),
        )
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .background(Brush.verticalGradient(colors = brushColors)),
        )
        Column {
            if (showBack) {
                LeaderboardTitle(onBack)
            }
            TrophyGallery(
                isLoading = loading,
                leaderboard = if (loading) emptyList() else state.leaderboard.map { it.toSharedEntry() },
                selectedMode = sharedMode,
                selectMode = { viewModel.selectMode(it.toFeatureMode(), countryCode) },
                countDownMs = state.countDownMs,
                blinkCountDown = state.blinkCountDown,
                openHistory = {
                    trackOpenHistory()
                    component.openDailyHistory()
                },
                isTrophyVisible = isTrophyVisible,
                rewardCurrency = sharedRewardCurrency,
                rewardCurrencyCode = state.rewardCurrencyCode,
                rewardsTable = state.rewardsTable,
                isTitleVisible = showBack,
            )
            if (state.leaderboard.isNotEmpty()) {
                LeaderboardTableHeader(
                    isTrophyVisible = isTrophyVisible,
                    rewardCurrency = sharedRewardCurrency,
                )
            }
        }
    }
}

private fun LeaderboardMode.toSharedMode(): SharedLeaderboardMode =
    when (this) {
        LeaderboardMode.DAILY -> SharedLeaderboardMode.DAILY
        LeaderboardMode.ALL_TIME -> SharedLeaderboardMode.ALL_TIME
    }

private fun SharedLeaderboardMode.toFeatureMode(): LeaderboardMode =
    when (this) {
        SharedLeaderboardMode.DAILY -> LeaderboardMode.DAILY
        SharedLeaderboardMode.ALL_TIME -> LeaderboardMode.ALL_TIME
    }

private fun RewardCurrency.toSharedRewardCurrency(): SharedRewardCurrency =
    when (this) {
        RewardCurrency.YRAL -> SharedRewardCurrency.YRAL
        RewardCurrency.BTC -> SharedRewardCurrency.BTC
    }

private fun LeaderboardItem.toSharedEntry(): LeaderboardEntry =
    LeaderboardEntry(
        principalId = userPrincipalId,
        username = username,
        profileImageUrl = profileImage,
        wins = wins,
        position = position,
        reward = reward,
    )
