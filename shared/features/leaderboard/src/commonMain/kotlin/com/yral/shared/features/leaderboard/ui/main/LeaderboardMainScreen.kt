package com.yral.shared.features.leaderboard.ui.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yral.shared.features.leaderboard.data.models.LeaderboardMode
import com.yral.shared.features.leaderboard.nav.main.LeaderboardMainComponent
import com.yral.shared.features.leaderboard.ui.LeaderboardRow
import com.yral.shared.features.leaderboard.ui.LeaderboardTableHeader
import com.yral.shared.features.leaderboard.viewmodel.LeaderBoardState
import com.yral.shared.features.leaderboard.viewmodel.LeaderBoardViewModel
import com.yral.shared.libs.designsystem.component.lottie.LottieRes
import com.yral.shared.libs.designsystem.component.lottie.YralLottieAnimation
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import yral_mobile.shared.features.leaderboard.generated.resources.Res
import yral_mobile.shared.features.leaderboard.generated.resources.purple_leaderboard
import yral_mobile.shared.features.leaderboard.generated.resources.yellow_leaderboard
import kotlin.math.max
import kotlin.math.min

@Suppress("LongMethod", "UnusedParameter", "CyclomaticComplexMethod")
@Composable
fun LeaderboardMainScreen(
    component: LeaderboardMainComponent,
    modifier: Modifier = Modifier,
    viewModel: LeaderBoardViewModel = koinViewModel(),
) {
    val countryCode = Locale.current.region
    val state by viewModel.state.collectAsState()
    var showConfetti by remember(state.isCurrentUserInTop) { mutableStateOf(state.isCurrentUserInTop) }
    LaunchedEffect(Unit) {
        viewModel.leaderboardPageViewed()
        viewModel.loadData(countryCode)
    }

    val listState = rememberLazyListState()
    var isTrophyVisible by remember { mutableStateOf(false) }
    LaunchedEffect(listState) {
        snapshotFlow { listState.canScrollBackward }
            .collect { canScrollBackward ->
                isTrophyVisible = !canScrollBackward
            }
    }
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
    Box(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
        ) {
            stickyHeader {
                LeaderboardHeader(
                    countryCode = countryCode,
                    state = state,
                    component = component,
                    isTrophyVisible = isTrophyVisible || state.isLoading,
                    viewModel = viewModel,
                    leaderboardBG = leaderboardBG,
                    trackOpenHistory = { viewModel.leaderboardCalendarClicked() },
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
                                userPrincipalId = user.userPrincipalId,
                                profileImageUrl = user.profileImage,
                                wins = user.wins,
                                isCurrentUser = true,
                                decorateCurrentUser = true,
                                rewardCurrency = state.rewardCurrency,
                                rewardCurrencyCode = state.rewardCurrencyCode,
                                reward = user.reward,
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
                            userPrincipalId = item.userPrincipalId,
                            profileImageUrl = item.profileImage,
                            wins = item.wins,
                            isCurrentUser = viewModel.isCurrentUser(item.userPrincipalId),
                            decorateCurrentUser = false,
                            rewardCurrency = state.rewardCurrency,
                            rewardCurrencyCode = state.rewardCurrencyCode,
                            reward = item.reward,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
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
    }
}

@Composable
private fun LeaderboardHeader(
    countryCode: String,
    state: LeaderBoardState,
    component: LeaderboardMainComponent,
    isTrophyVisible: Boolean,
    viewModel: LeaderBoardViewModel,
    leaderboardBG: DrawableResource,
    trackOpenHistory: () -> Unit,
) {
    val brushColors =
        when (state.selectedMode) {
            LeaderboardMode.DAILY -> LeaderboardMainScreenConstants.YELLOW_BRUSH
            LeaderboardMode.ALL_TIME -> LeaderboardMainScreenConstants.PURPLE_BRUSH
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
            TrophyGallery(
                isLoading = state.isLoading,
                leaderboard = if (state.isLoading) emptyList() else state.leaderboard,
                selectedMode = state.selectedMode,
                selectMode = { viewModel.selectMode(it, countryCode) },
                countDownMs = state.countDownMs,
                blinkCountDown = state.blinkCountDown,
                openHistory = {
                    trackOpenHistory()
                    component.openDailyHistory()
                },
                isTrophyVisible = isTrophyVisible,
                rewardCurrency = state.rewardCurrency,
                rewardCurrencyCode = state.rewardCurrencyCode,
                rewardsTable = state.rewardsTable,
            )
            LeaderboardTableHeader(
                isTrophyVisible = isTrophyVisible,
                rewardCurrency = state.rewardCurrency,
            )
        }
    }
}

@Composable
fun LeaderboardConfetti(
    showConfetti: Boolean,
    confettiAnimationComplete: () -> Unit,
) {
    if (showConfetti) {
        var count by remember { mutableIntStateOf(0) }
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            val size = LocalWindowInfo.current.containerSize.width / LeaderboardMainScreenConstants.CONFETTI_SIZE_FACTOR
            val density = LocalDensity.current
            repeat(LeaderboardMainScreenConstants.NO_OF_CONFETTI) { index ->
                key(count) {
                    YralLottieAnimation(
                        rawRes = LottieRes.COLORFUL_CONFETTI_BRUST,
                        contentScale = ContentScale.Crop,
                        iterations = 1,
                        onAnimationComplete = {
                            if (index == 0) {
                                if (count < LeaderboardMainScreenConstants.CONFETTI_ITERATIONS) {
                                    count++
                                } else {
                                    confettiAnimationComplete()
                                }
                            }
                        },
                        modifier =
                            Modifier
                                .size(with(density) { size.toDp() })
                                .scale(LeaderboardMainScreenConstants.CONFETTI_SCALE)
                                .align(if (index % 2 == 0) Alignment.Start else Alignment.End),
                    )
                }
            }
        }
    }
}

@Suppress("MagicNumber")
object LeaderboardMainScreenConstants {
    val LEADERBOARD_HEADER_WEIGHTS = listOf(0.17f, 0.52f, 0.30f, 0.29f)
    val LEADERBOARD_HEADER_WEIGHTS_FOLD = listOf(0.17f, 0.54f, 0.30f, 0.27f)
    val LEADERBOARD_ROW_WEIGHTS = listOf(0.17f, 0.55f, 0.30f, 0.26f)
    const val MAX_CHAR_OF_NAME = 9
    const val COUNT_DOWN_BG_ALPHA = 0.8f
    const val COUNT_DOWN_ANIMATION_DURATION = 500
    const val COUNT_DOWN_BORDER_ANIMATION_DURATION = 300
    val YELLOW_BRUSH =
        listOf(
            Color(0x00FFC842).copy(alpha = 0f),
            Color(0xFFF6B517).copy(alpha = 0.7f),
        )
    val PURPLE_BRUSH =
        listOf(
            Color(0x00706EBB).copy(alpha = 0f),
            Color(0xFF7573BD).copy(alpha = 0.7f),
        )

    const val CONFETTI_SCALE = 2.5f
    const val NO_OF_CONFETTI = 5
    const val CONFETTI_SIZE_FACTOR = 3
    const val CONFETTI_ITERATIONS = 0
}

@Composable
expect fun isScreenUnfolded(): Boolean
