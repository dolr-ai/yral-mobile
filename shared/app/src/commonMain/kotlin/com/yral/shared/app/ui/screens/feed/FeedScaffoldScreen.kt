package com.yral.shared.app.ui.screens.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yral.shared.analytics.events.GameType
import com.yral.shared.app.ui.screens.feed.FeedScaffoldScreenConstants.TOP_OVERLAY_ITEM_OFFSET_X
import com.yral.shared.app.ui.screens.feed.performance.PrefetchVideoListenerImpl
import com.yral.shared.app.ui.screens.feed.performance.VideoListenerImpl
import com.yral.shared.features.feed.nav.FeedComponent
import com.yral.shared.features.feed.ui.FeedScreen
import com.yral.shared.features.feed.ui.components.FeedOnboardingNudge
import com.yral.shared.features.feed.ui.components.FeedTargetBounds
import com.yral.shared.features.feed.ui.components.UserBrief
import com.yral.shared.features.feed.ui.components.captureFeedOnboardingBounds
import com.yral.shared.features.feed.viewmodel.FeedState
import com.yral.shared.features.feed.viewmodel.FeedViewModel
import com.yral.shared.features.feed.viewmodel.OnboardingStep
import com.yral.shared.features.feed.viewmodel.OverlayType
import com.yral.shared.features.game.domain.models.VoteResult
import com.yral.shared.features.game.ui.AboutGameSheet
import com.yral.shared.features.game.ui.CoinBalance
import com.yral.shared.features.game.ui.Game
import com.yral.shared.features.game.ui.GameResultSheet
import com.yral.shared.features.game.ui.GameToggle
import com.yral.shared.features.game.ui.HowToPlay
import com.yral.shared.features.game.ui.RefreshBalanceAnimation
import com.yral.shared.features.game.ui.toRefreshBalanceAnimationState
import com.yral.shared.features.game.viewmodel.GameState
import com.yral.shared.features.game.viewmodel.GameViewModel
import com.yral.shared.features.game.viewmodel.NudgeType
import com.yral.shared.features.leaderboard.ui.DailyRanK
import com.yral.shared.features.leaderboard.viewmodel.LeaderBoardViewModel
import com.yral.shared.libs.designsystem.component.lottie.PreloadLottieAnimations
import com.yral.shared.rust.service.domain.models.toCanisterData
import com.yral.shared.rust.service.utils.CanisterData
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.app.generated.resources.Res
import yral_mobile.shared.app.generated.resources.onboarding_nudge_balance
import yral_mobile.shared.app.generated.resources.onboarding_nudge_balance_highlight
import yral_mobile.shared.app.generated.resources.onboarding_nudge_rank
import yral_mobile.shared.app.generated.resources.onboarding_nudge_rank_highlight
import yral_mobile.shared.libs.designsystem.generated.resources.shadow
import kotlin.time.Duration.Companion.seconds
import com.yral.shared.features.feed.ui.components.ArrowAlignment as FeedArrowAlignment
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
fun FeedScaffoldScreen(
    component: FeedComponent,
    feedViewModel: FeedViewModel,
    gameViewModel: GameViewModel,
    leaderBoardViewModel: LeaderBoardViewModel,
) {
    val gameState by gameViewModel.state.collectAsStateWithLifecycle()
    val feedState by feedViewModel.state.collectAsStateWithLifecycle()
    val dailyRank by if (feedState.overlayType == OverlayType.DAILY_RANK) {
        leaderBoardViewModel.dailyRank.collectAsStateWithLifecycle(null)
    } else {
        remember { mutableStateOf(null) }
    }
    if (feedState.overlayType == OverlayType.DAILY_RANK) {
        leaderBoardViewModel.refreshRank.collectAsStateWithLifecycle(false)
    }
    FeedScreen(
        component = component,
        viewModel = feedViewModel,
        topOverlay = { pageNo ->
            OverLayTop(
                pageNo = pageNo,
                dailyRank = dailyRank,
                feedState = feedState,
                gameState = gameState,
                componentAnimationInfo = TopComponentAnimationInfo(gameState.animateCoinBalance),
                setAnimateCoinBalance = { gameViewModel.setAnimateCoinBalance(it) },
                setPostDescriptionExpanded = { feedViewModel.setPostDescriptionExpanded(it) },
                updateGameType = { gameViewModel.updateGameType(it) },
                openUserProfile = { component.openProfile(it) },
                feedViewModel = feedViewModel,
                openLeaderboard = { component.openLeaderboard() },
                openWallet = { component.openWallet() },
            )
        },
        bottomOverlay = { pageNo, scrollToNext ->
            OverlayBottom(pageNo, feedState, gameState, gameViewModel, feedViewModel, scrollToNext)
        },
        onPageChanged = { pageNo, currentPageOfFeed ->
            if (pageNo >= 0 && pageNo < feedState.feedDetails.size) {
                // Set current video ID for the new page
                gameViewModel.setCurrentVideoId(feedState.feedDetails[pageNo].videoID)
                // Mark animation as shown for the previous page when changing pages
                if (pageNo != currentPageOfFeed && currentPageOfFeed < feedState.feedDetails.size) {
                    gameViewModel.markCoinDeltaAnimationShown(
                        videoId = feedState.feedDetails[currentPageOfFeed].videoID,
                    )
                }
            }
            gameViewModel.showNudge(
                nudgeIntention = NudgeType.INTRO,
                pageNo = pageNo,
                feedDetailsSize = feedState.feedDetails.size,
            )
        },
        onEdgeScrollAttempt = { pageNo ->
            // Don't show mandatory nudge during onboarding
            if (feedState.currentOnboardingStep == null) {
                gameViewModel.showNudge(
                    nudgeIntention = NudgeType.MANDATORY,
                    pageNo = pageNo,
                    feedDetailsSize = feedState.feedDetails.size,
                )
            }
        },
        limitReelCount =
            if (gameState.isStopAndVote || feedState.currentOnboardingStep != null) {
                gameState.lastVotedCount
            } else {
                feedState.feedDetails.size
            },
        getPrefetchListener = { PrefetchVideoListenerImpl(it) },
        getVideoListener = {
            VideoListenerImpl(
                reel = it,
                registerTrace = { id, tractType ->
                    feedViewModel.registerTrace(id, tractType.name)
                },
                isTraced = { id, traceType ->
                    feedViewModel.isAlreadyTraced(id, traceType.name)
                },
            )
        },
    )
    RefreshBalanceAnimation(
        refreshBalanceState = gameState.refreshBalanceState.toRefreshBalanceAnimationState(),
        onAnimationComplete = { gameViewModel.hideRefreshBalanceAnimation() },
    )
    if (gameState.showResultSheet && feedState.currentPageOfFeed < feedState.feedDetails.size) {
        val currentVideoId = feedState.feedDetails[feedState.currentPageOfFeed].videoID
        val coinDelta = gameViewModel.getFeedGameResult(currentVideoId)
        if (coinDelta != 0) {
            GameResultSheet(
                coinDelta = coinDelta,
                gameIcon = gameState.gameResult[currentVideoId]?.first,
                onDismissRequest = {
                    gameViewModel.toggleResultSheet(false)
                },
                openAboutGame = {
                    gameViewModel.toggleAboutGame(true)
                },
                onSheetButtonClicked = { ctaType ->
                    gameViewModel.onResultSheetButtonClicked(
                        coinDelta = coinDelta,
                        ctaType = ctaType,
                    )
                },
            )
        } else {
            gameViewModel.toggleResultSheet(false)
        }
    }
    if (gameState.showAboutGame && feedState.currentPageOfFeed < feedState.feedDetails.size) {
        AboutGameSheet(
            gameRules = gameState.gameRules,
            onDismissRequest = {
                gameViewModel.toggleAboutGame(false)
            },
        )
    }
    if (gameState.gameIcons.isNotEmpty()) {
        PreloadLottieAnimations(
            urls = gameState.gameIcons.map { it.clickAnimation },
        )
    }
    LaunchedEffect(
        feedState.feedDetails.size,
        gameState.gameIcons.size,
        feedState.currentOnboardingStep,
    ) {
        @Suppress("ComplexCondition")
        // Only show mandatory nudge after onboarding is complete
        if (feedState.feedDetails.isNotEmpty() &&
            gameState.gameIcons.isNotEmpty() &&
            !gameState.isDefaultMandatoryNudgeShown &&
            feedState.currentOnboardingStep == null
        ) {
            gameViewModel.showNudge(
                nudgeIntention = NudgeType.MANDATORY,
                pageNo = feedState.currentPageOfFeed,
                feedDetailsSize = feedState.feedDetails.size,
            )
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun OverLayTop(
    pageNo: Int,
    dailyRank: Long?,
    feedState: FeedState,
    gameState: GameState,
    componentAnimationInfo: TopComponentAnimationInfo,
    setAnimateCoinBalance: (Boolean) -> Unit,
    setPostDescriptionExpanded: (Boolean) -> Unit,
    updateGameType: (GameType) -> Unit,
    openUserProfile: (canisterData: CanisterData) -> Unit,
    feedViewModel: FeedViewModel,
    openLeaderboard: () -> Unit,
    openWallet: () -> Unit,
) {
    var targetBounds by remember { mutableStateOf<FeedTargetBounds?>(null) }
    Box(modifier = Modifier.fillMaxSize()) {
        when (feedState.overlayType) {
            OverlayType.DEFAULT -> {
                OverlayTopDefault(
                    pageNo = pageNo,
                    feedState = feedState,
                    gameState = gameState,
                    componentInfo = componentAnimationInfo,
                    setPostDescriptionExpanded = setPostDescriptionExpanded,
                    setAnimateCoinBalance = setAnimateCoinBalance,
                    openUserProfile = openUserProfile,
                    openWallet = openWallet,
                )
            }
            OverlayType.GAME_TOGGLE -> {
                OverlayTopGameToggle(
                    gameState = gameState,
                    setAnimateCoinBalance = setAnimateCoinBalance,
                    updateGameType = updateGameType,
                    openWallet = openWallet,
                )
            }
            OverlayType.DAILY_RANK ->
                OverlayTopDailyRank(
                    dailyRank = dailyRank,
                    feedState = feedState,
                    gameState = gameState,
                    setAnimateCoinBalance = setAnimateCoinBalance,
                    onTargetBoundsCaptured = { targetBounds = it },
                    openLeaderboard = openLeaderboard,
                    openWallet = openWallet,
                )
        }
        if (feedState.currentPageOfFeed > 0) {
            when (feedState.currentOnboardingStep) {
                OnboardingStep.INTRO_RANK -> {
                    dailyRank?.let {
                        FeedOnboardingNudge(
                            text = stringResource(Res.string.onboarding_nudge_rank),
                            highlightText = stringResource(Res.string.onboarding_nudge_rank_highlight),
                            arrowAlignment = FeedArrowAlignment.TOP_START,
                            isDismissible = false,
                            targetBounds = targetBounds,
                            onDismiss = { feedViewModel.dismissOnboardingStep() },
                        )
                    }
                }

                OnboardingStep.INTRO_BALANCE -> {
                    FeedOnboardingNudge(
                        text = stringResource(Res.string.onboarding_nudge_balance),
                        highlightText = stringResource(Res.string.onboarding_nudge_balance_highlight),
                        arrowAlignment = FeedArrowAlignment.TOP_END,
                        isDismissible = false,
                        targetBounds = targetBounds,
                        onDismiss = { feedViewModel.dismissOnboardingStep() },
                    )
                }

                else -> {}
            }
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun OverlayTopDefault(
    pageNo: Int,
    feedState: FeedState,
    gameState: GameState,
    componentInfo: TopComponentAnimationInfo,
    setPostDescriptionExpanded: (Boolean) -> Unit,
    setAnimateCoinBalance: (Boolean) -> Unit,
    openUserProfile: (canisterData: CanisterData) -> Unit,
    openWallet: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .paint(
                    painter = painterResource(DesignRes.drawable.shadow),
                    contentScale = ContentScale.FillBounds,
                ).padding(end = 26.dp),
    ) {
        var paddingEnd by remember { mutableFloatStateOf(0f) }
        val density = LocalDensity.current
        val screenWidthPx = LocalWindowInfo.current.containerSize.width
        val feedDetails = feedState.feedDetails[pageNo]
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
                    .padding(end = with(density) { paddingEnd.toDp() + 46.dp })
                    .clickable { openUserProfile(feedDetails.toCanisterData()) },
        )
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(vertical = 10.dp)
                    .onGloballyPositioned { coordinates ->
                        if (!componentInfo.isTopRightAnimating) {
                            val x = coordinates.positionInParent().x
                            paddingEnd = screenWidthPx - x
                        }
                    },
        ) {
            CoinBalance(
                coinBalance = gameState.coinBalance,
                coinDelta = gameState.lastBalanceDifference,
                animateBag = gameState.animateCoinBalance,
                setAnimate = { setAnimateCoinBalance(it) },
                modifier = Modifier.padding(vertical = 22.dp).clickable { openWallet() },
            )
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun OverlayTopDailyRank(
    dailyRank: Long?,
    feedState: FeedState,
    gameState: GameState,
    setAnimateCoinBalance: (Boolean) -> Unit,
    onTargetBoundsCaptured: (FeedTargetBounds?) -> Unit,
    openLeaderboard: () -> Unit,
    openWallet: () -> Unit,
) {
    val density = LocalDensity.current
    val isShowingRankNudge = feedState.currentOnboardingStep == OnboardingStep.INTRO_RANK
    val isShowingBalanceNudge = feedState.currentOnboardingStep == OnboardingStep.INTRO_BALANCE
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .paint(
                    painter = painterResource(DesignRes.drawable.shadow),
                    contentScale = ContentScale.FillBounds,
                ).padding(horizontal = 26.dp),
    ) {
        dailyRank?.let {
            DailyRanK(
                position = dailyRank,
                animate = gameState.animateCoinBalance,
                setAnimate = { setAnimateCoinBalance(it) },
                isShowingNudge = isShowingRankNudge,
                modifier =
                    Modifier
                        .padding(vertical = 32.dp)
                        .align(Alignment.TopStart)
                        .clickable { openLeaderboard() }
                        .then(
                            if (isShowingRankNudge) {
                                Modifier
                                    .captureFeedOnboardingBounds(
                                        targetBounds = { onTargetBoundsCaptured(it) },
                                        adjustBounds = { bounds ->
                                            val trophyOffsetPx = with(density) { TOP_OVERLAY_ITEM_OFFSET_X.dp.toPx() }
                                            FeedTargetBounds(
                                                left = bounds.left,
                                                top = bounds.top,
                                                right = bounds.right + trophyOffsetPx,
                                                bottom = bounds.bottom,
                                            )
                                        },
                                    )
                            } else {
                                Modifier
                            },
                        ),
            )
        }
        CoinBalance(
            coinBalance = gameState.coinBalance,
            coinDelta = gameState.lastBalanceDifference,
            animateBag = gameState.animateCoinBalance,
            setAnimate = { setAnimateCoinBalance(it) },
            isShowingNudge = isShowingBalanceNudge,
            modifier =
                Modifier
                    .padding(vertical = 32.dp)
                    .align(Alignment.TopEnd)
                    .clickable { openWallet() }
                    .then(
                        if (isShowingBalanceNudge) {
                            Modifier
                                .captureFeedOnboardingBounds(
                                    targetBounds = { onTargetBoundsCaptured(it) },
                                    adjustBounds = { bounds ->
                                        val bagOffsetPx = with(density) { TOP_OVERLAY_ITEM_OFFSET_X.dp.toPx() }
                                        FeedTargetBounds(
                                            left = bounds.left - bagOffsetPx,
                                            top = bounds.top,
                                            right = bounds.right,
                                            bottom = bounds.bottom,
                                        )
                                    },
                                )
                        } else {
                            Modifier
                        },
                    ),
        )
    }
}

@Composable
private fun OverlayTopGameToggle(
    gameState: GameState,
    setAnimateCoinBalance: (Boolean) -> Unit,
    updateGameType: (GameType) -> Unit,
    openWallet: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .paint(
                    painter = painterResource(DesignRes.drawable.shadow),
                    contentScale = ContentScale.FillBounds,
                ),
        contentAlignment = Alignment.TopStart,
    ) {
        GameToggle(
            gameType = gameState.gameType,
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 22.dp),
        ) { updateGameType(it) }
        Row(
            modifier =
                Modifier
                    .padding(start = 26.dp, end = 26.dp, top = 11.dp)
                    .align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(modifier = Modifier.weight(1f))
            CoinBalance(
                coinBalance = gameState.coinBalance,
                coinDelta = gameState.lastBalanceDifference,
                animateBag = gameState.animateCoinBalance,
                setAnimate = { setAnimateCoinBalance(it) },
                modifier = Modifier.padding(vertical = 22.dp).clickable { openWallet() },
            )
        }
    }
}

data class TopComponentAnimationInfo(
    val isTopRightAnimating: Boolean,
)

@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
private fun OverlayBottom(
    pageNo: Int,
    feedState: FeedState,
    gameState: GameState,
    gameViewModel: GameViewModel,
    feedViewModel: FeedViewModel,
    scrollToNext: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (gameState.gameIcons.isNotEmpty()) {
            HowToPlay(
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 105.dp),
                shouldExpand =
                    pageNo < gameState.isHowToPlayShown.size &&
                        !gameState.isHowToPlayShown[pageNo] &&
                        pageNo == feedState.currentPageOfFeed,
                pageNo = pageNo,
                onClick = { gameViewModel.toggleAboutGame(true) },
                onAnimationComplete = { gameViewModel.setHowToPlayShown(pageNo, feedState.currentPageOfFeed) },
            )
        }
        if (pageNo < feedState.feedDetails.size) {
            // Map OnboardingStep to NudgeType for game module
            val onboardingNudgeType =
                when (feedState.currentOnboardingStep) {
                    OnboardingStep.INTRO_GAME -> NudgeType.ONBOARDING_START
                    OnboardingStep.INTRO_GAME_END -> NudgeType.ONBOARDING_END
                    null -> null
                    else -> NudgeType.ONBOARDING_OTHERS
                }
            Game(
                feedDetails = feedState.feedDetails[pageNo],
                pageNo = pageNo,
                gameViewModel = gameViewModel,
                onboardingNudgeType = onboardingNudgeType,
                onOnboardingNudgeComplete = { feedViewModel.dismissOnboardingStep() },
            )
            if (gameState.isAutoScrollEnabled || feedState.currentOnboardingStep != null) {
                var resultOfCurrentPage by remember { mutableStateOf<VoteResult?>(null) }
                val currentVideoId = feedState.feedDetails[pageNo].videoID
                val voteResult = gameState.gameResult[currentVideoId]?.second

                // Track when result becomes available for the current page
                LaunchedEffect(voteResult?.coinDelta, pageNo) {
                    if (voteResult != null && pageNo == feedState.currentPageOfFeed) {
                        resultOfCurrentPage = voteResult
                    }
                }

                // Auto-scroll logic: wait for result sheet to be dismissed if it should be shown
                LaunchedEffect(
                    resultOfCurrentPage?.coinDelta,
                    gameState.showResultSheet,
                    pageNo,
                ) {
                    val resultCoinDelta = resultOfCurrentPage?.coinDelta ?: 0
                    val resultHasShownAnimation = resultOfCurrentPage?.hasShownAnimation ?: false
                    val isCurrentPage = pageNo == feedState.currentPageOfFeed

                    // Check if result sheet is being shown for this page
                    val isResultSheetShown = gameState.showResultSheet && resultCoinDelta != 0 && isCurrentPage

                    // Only auto-scroll if:
                    // 1. There's a result with non-zero coinDelta
                    // 2. Animation hasn't been shown
                    // 3. It's the current page
                    // 4. Result sheet is not currently being shown (has been dismissed or doesn't need to be shown)
                    val shouldAutoScroll =
                        resultCoinDelta != 0 &&
                            !resultHasShownAnimation &&
                            isCurrentPage &&
                            !isResultSheetShown

                    if (shouldAutoScroll) {
                        delay(1.seconds)
                        scrollToNext()
                    }
                }
            }
        }
    }
}

private object FeedScaffoldScreenConstants {
    const val TOP_OVERLAY_ITEM_OFFSET_X = 18f
}
