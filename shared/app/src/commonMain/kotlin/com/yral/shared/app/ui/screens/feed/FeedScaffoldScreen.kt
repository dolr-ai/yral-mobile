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
import androidx.compose.runtime.derivedStateOf
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
import com.yral.shared.analytics.events.SwipeAction
import com.yral.shared.app.ui.screens.feed.FeedScaffoldScreenConstants.TOP_OVERLAY_ITEM_OFFSET_X
import com.yral.shared.features.feed.nav.FeedComponent
import com.yral.shared.features.feed.ui.FeedActionsRight
import com.yral.shared.features.feed.ui.FeedScreen
import com.yral.shared.features.feed.ui.components.FeedOnboardingNudge
import com.yral.shared.features.feed.ui.components.FeedTargetBounds
import com.yral.shared.features.feed.ui.components.UserBrief
import com.yral.shared.features.feed.ui.components.captureFeedOnboardingBounds
import com.yral.shared.features.feed.viewmodel.FeedViewModel
import com.yral.shared.features.feed.viewmodel.OnboardingStep
import com.yral.shared.features.feed.viewmodel.OverlayType
import com.yral.shared.features.game.domain.models.VoteResult
import com.yral.shared.features.game.ui.AboutGameSheet
import com.yral.shared.features.game.ui.CoinBalance
import com.yral.shared.features.game.ui.Game
import com.yral.shared.features.game.ui.GameResultSheet
import com.yral.shared.features.game.ui.GameToggle
import com.yral.shared.features.game.ui.HotOrNotOnboardingOverlay
import com.yral.shared.features.game.ui.HotOrNotResultOverlay
import com.yral.shared.features.game.ui.HowToPlay
import com.yral.shared.features.game.ui.RefreshBalanceAnimation
import com.yral.shared.features.game.ui.toRefreshBalanceAnimationState
import com.yral.shared.features.game.viewmodel.GameViewModel
import com.yral.shared.features.game.viewmodel.NudgeType
import com.yral.shared.features.leaderboard.ui.DailyRanK
import com.yral.shared.features.leaderboard.viewmodel.LeaderBoardViewModel
import com.yral.shared.features.tournament.ui.TournamentIntroBottomSheet
import com.yral.shared.libs.designsystem.component.lottie.PreloadLottieAnimations
import com.yral.shared.libs.videoPlayer.cardstack.SwipeDirection
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
    onNavigateToTournaments: () -> Unit,
) {
    // State collected here is ONLY used for sheets/effects — NOT passed into FeedScreen lambdas.
    val gameState by gameViewModel.state.collectAsStateWithLifecycle()
    val feedState by feedViewModel.state.collectAsStateWithLifecycle()
    val gameIcons by gameViewModel.gameIcons.collectAsStateWithLifecycle()

    // derivedStateOf: only re-emit when the computed Int actually changes.
    val limitReelCount by remember {
        derivedStateOf {
            if (gameState.isStopAndVote || feedState.currentOnboardingStep != null) {
                gameState.lastVotedCount
            } else {
                feedState.feedDetails.size
            }
        }
    }

    // isCardLayoutEnabled is constant for the ViewModel lifetime, read once.
    val isCardLayoutEnabled = feedState.isCardLayoutEnabled

    // ── FeedScreen — all lambdas capture only stable ViewModel / component refs ──
    FeedScreen(
        component = component,
        viewModel = feedViewModel,
        topOverlay = { pageNo ->
            OverLayTop(
                pageNo = pageNo,
                feedViewModel = feedViewModel,
                gameViewModel = gameViewModel,
                leaderBoardViewModel = leaderBoardViewModel,
                openUserProfile = component::openProfile,
                openLeaderboard = component.openLeaderboard,
                openWallet = component.openWallet,
            )
        },
        bottomOverlay = { pageNo, scrollToNext ->
            OverlayBottom(
                pageNo = pageNo,
                gameViewModel = gameViewModel,
                feedViewModel = feedViewModel,
                scrollToNext = scrollToNext,
            )
        },
        actionsRight = { pageNo ->
            FeedActionsRight(
                pageNo = pageNo,
                feedViewModel = feedViewModel,
                openProfile = component::openProfile,
            )
        },
        // Event callbacks read state.value at invocation time — no captured state values.
        onPageChanged = { pageNo, currentPageOfFeed ->
            val details = feedViewModel.state.value.feedDetails
            if (pageNo >= 0 && pageNo < details.size) {
                gameViewModel.setCurrentVideoId(details[pageNo].videoID)
                if (pageNo != currentPageOfFeed && currentPageOfFeed < details.size) {
                    gameViewModel.markCoinDeltaAnimationShown(
                        videoId = details[currentPageOfFeed].videoID,
                    )
                    if (feedViewModel.state.value.isCardLayoutEnabled) {
                        gameViewModel.markHotOrNotAnimationShown(
                            details[currentPageOfFeed].videoID,
                        )
                    }
                }
            }
            gameViewModel.showNudge(
                nudgeIntention = NudgeType.INTRO,
                pageNo = pageNo,
                feedDetailsSize = details.size,
            )
        },
        onEdgeScrollAttempt = { pageNo ->
            if (feedViewModel.state.value.currentOnboardingStep == null) {
                gameViewModel.showNudge(
                    nudgeIntention = NudgeType.MANDATORY,
                    pageNo = pageNo,
                    feedDetailsSize = feedViewModel.state.value.feedDetails.size,
                )
            }
        },
        limitReelCount = limitReelCount,
        onSwipeVote =
            if (isCardLayoutEnabled) {
                { direction, pageIndex, isSwipe ->
                    val details = feedViewModel.state.value.feedDetails
                    if (pageIndex < details.size) {
                        val isHot = direction == SwipeDirection.RIGHT
                        gameViewModel.castHotOrNotVote(
                            isHot = isHot,
                            feedDetails = details[pageIndex],
                            swipeAction = if (isSwipe) SwipeAction.SWIPE else SwipeAction.CLICK,
                        )
                    }
                }
            } else {
                null
            },
    )

    // ── Sheets & effects — sibling of FeedScreen, own recomposition scope ──
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
            isHotOrNotMode = feedState.isCardLayoutEnabled,
        )
    }
    val lottieUrls = remember(gameIcons) { gameIcons.map { it.clickAnimation } }
    if (lottieUrls.isNotEmpty()) {
        PreloadLottieAnimations(urls = lottieUrls)
    }
    LaunchedEffect(
        feedState.feedDetails.size,
        gameIcons.size,
        feedState.currentOnboardingStep,
    ) {
        @Suppress("ComplexCondition")
        if (feedState.feedDetails.isNotEmpty() &&
            gameIcons.isNotEmpty() &&
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
    LaunchedEffect(feedState.currentPageOfFeed, feedState.currentOnboardingStep) {
        if (feedState.feedDetails.isNotEmpty() &&
            feedState.currentOnboardingStep == null &&
            feedState.currentPageOfFeed == FeedViewModel.TOURNAMENT_INTRO_PAGE
        ) {
            feedViewModel.checkAndShowTournamentIntroSheet()
        }
    }
    LaunchedEffect(feedState.feedDetails.size, feedState.isCardLayoutEnabled) {
        if (feedState.feedDetails.isNotEmpty() &&
            feedState.isCardLayoutEnabled &&
            feedState.currentOnboardingStep == null
        ) {
            feedViewModel.checkAndShowHotOrNotOnboarding()
        }
    }
    if (feedState.showTournamentIntroSheet) {
        TournamentIntroBottomSheet(
            onDismissRequest = { feedViewModel.dismissTournamentIntroSheet() },
            onViewTournamentsClick = {
                feedViewModel.dismissTournamentIntroSheet()
                onNavigateToTournaments()
            },
        )
    }
    if (feedState.showHotOrNotOnboarding) {
        HotOrNotOnboardingOverlay(
            onDismiss = { feedViewModel.dismissHotOrNotOnboarding() },
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
//  Overlay composables — each collects only the state it needs from ViewModels
// ──────────────────────────────────────────────────────────────────────────────

@Suppress("LongMethod")
@Composable
private fun OverLayTop(
    pageNo: Int,
    feedViewModel: FeedViewModel,
    gameViewModel: GameViewModel,
    leaderBoardViewModel: LeaderBoardViewModel,
    openUserProfile: (canisterData: CanisterData) -> Unit,
    openLeaderboard: () -> Unit,
    openWallet: () -> Unit,
) {
    val feedState by feedViewModel.state.collectAsStateWithLifecycle()
    val gameState by gameViewModel.state.collectAsStateWithLifecycle()

    // Daily rank — collected locally so the lambda capturing OverLayTop stays stable
    val smileyDailyRank by if (feedState.overlayType == OverlayType.DAILY_RANK) {
        leaderBoardViewModel.dailyRank.collectAsStateWithLifecycle(null)
    } else {
        remember { mutableStateOf(null) }
    }
    val honDailyRank by if (feedState.overlayType == OverlayType.DAILY_RANK) {
        leaderBoardViewModel.honDailyRank.collectAsStateWithLifecycle(null)
    } else {
        remember { mutableStateOf(null) }
    }
    val dailyRank =
        if (feedState.isCardLayoutEnabled) {
            honDailyRank ?: 0L
        } else {
            smileyDailyRank
        }
    if (feedState.overlayType == OverlayType.DAILY_RANK) {
        leaderBoardViewModel.refreshRank.collectAsStateWithLifecycle(false)
    }

    var targetBounds by remember { mutableStateOf<FeedTargetBounds?>(null) }
    Box(modifier = Modifier.fillMaxSize()) {
        when (feedState.overlayType) {
            OverlayType.DEFAULT -> {
                OverlayTopDefault(
                    pageNo = pageNo,
                    feedState = feedState,
                    coinBalance = gameState.coinBalance,
                    lastBalanceDifference = gameState.lastBalanceDifference,
                    animateCoinBalance = gameState.animateCoinBalance,
                    setPostDescriptionExpanded = { feedViewModel.setPostDescriptionExpanded(it) },
                    setAnimateCoinBalance = { gameViewModel.setAnimateCoinBalance(it) },
                    openUserProfile = openUserProfile,
                    openWallet = openWallet,
                )
            }
            OverlayType.GAME_TOGGLE -> {
                OverlayTopGameToggle(
                    gameType = gameState.gameType,
                    coinBalance = gameState.coinBalance,
                    lastBalanceDifference = gameState.lastBalanceDifference,
                    animateCoinBalance = gameState.animateCoinBalance,
                    setAnimateCoinBalance = { gameViewModel.setAnimateCoinBalance(it) },
                    updateGameType = { gameViewModel.updateGameType(it) },
                    openWallet = openWallet,
                )
            }
            OverlayType.DAILY_RANK ->
                OverlayTopDailyRank(
                    dailyRank = dailyRank,
                    currentOnboardingStep = feedState.currentOnboardingStep,
                    coinBalance = gameState.coinBalance,
                    lastBalanceDifference = gameState.lastBalanceDifference,
                    animateCoinBalance = gameState.animateCoinBalance,
                    setAnimateCoinBalance = { gameViewModel.setAnimateCoinBalance(it) },
                    onTargetBoundsCaptured = { targetBounds = it },
                    openLeaderboard = openLeaderboard,
                    openWallet = openWallet,
                )
        }
        if (feedState.currentPageOfFeed >= 0) {
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

@Suppress("LongMethod")
@Composable
private fun OverlayTopDefault(
    pageNo: Int,
    feedState: com.yral.shared.features.feed.viewmodel.FeedState,
    coinBalance: Long,
    lastBalanceDifference: Int,
    animateCoinBalance: Boolean,
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
                        if (!animateCoinBalance) {
                            val x = coordinates.positionInParent().x
                            paddingEnd = screenWidthPx - x
                        }
                    },
        ) {
            CoinBalance(
                coinBalance = coinBalance,
                coinDelta = lastBalanceDifference,
                animateBag = animateCoinBalance,
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
    currentOnboardingStep: OnboardingStep?,
    coinBalance: Long,
    lastBalanceDifference: Int,
    animateCoinBalance: Boolean,
    setAnimateCoinBalance: (Boolean) -> Unit,
    onTargetBoundsCaptured: (FeedTargetBounds?) -> Unit,
    openLeaderboard: () -> Unit,
    openWallet: () -> Unit,
) {
    val density = LocalDensity.current
    val isShowingRankNudge = currentOnboardingStep == OnboardingStep.INTRO_RANK
    val isShowingBalanceNudge = currentOnboardingStep == OnboardingStep.INTRO_BALANCE
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
                animate = animateCoinBalance,
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
            coinBalance = coinBalance,
            coinDelta = lastBalanceDifference,
            animateBag = animateCoinBalance,
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
    gameType: GameType,
    coinBalance: Long,
    lastBalanceDifference: Int,
    animateCoinBalance: Boolean,
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
            gameType = gameType,
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
                coinBalance = coinBalance,
                coinDelta = lastBalanceDifference,
                animateBag = animateCoinBalance,
                setAnimate = { setAnimateCoinBalance(it) },
                modifier = Modifier.padding(vertical = 22.dp).clickable { openWallet() },
            )
        }
    }
}

@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
private fun OverlayBottom(
    pageNo: Int,
    gameViewModel: GameViewModel,
    feedViewModel: FeedViewModel,
    scrollToNext: () -> Unit,
) {
    val feedState by feedViewModel.state.collectAsStateWithLifecycle()
    val gameState by gameViewModel.state.collectAsStateWithLifecycle()
    val gameIcons by gameViewModel.gameIcons.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        if (gameIcons.isNotEmpty()) {
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
                onClick = {
                    gameViewModel.onHowToPlayClicked()
                    gameViewModel.toggleAboutGame(true)
                },
                onAnimationComplete = { gameViewModel.setHowToPlayShown(pageNo, feedState.currentPageOfFeed) },
            )
        }
        if (pageNo < feedState.feedDetails.size) {
            val currentVideoId = feedState.feedDetails[pageNo].videoID

            val onboardingNudgeType =
                when (feedState.currentOnboardingStep) {
                    OnboardingStep.INTRO_GAME -> NudgeType.ONBOARDING_START
                    OnboardingStep.INTRO_GAME_END -> NudgeType.ONBOARDING_END
                    null -> null
                    else -> NudgeType.ONBOARDING_OTHERS
                }

            if (feedState.isCardLayoutEnabled) {
                val hotOrNotResult = gameViewModel.getHotOrNotResult(currentVideoId)
                if (hotOrNotResult != null) {
                    HotOrNotResultOverlay(
                        result = hotOrNotResult,
                        hasShownAnimation = gameViewModel.hasShownHotOrNotAnimation(currentVideoId),
                        onAnimationComplete = {
                            gameViewModel.markHotOrNotAnimationShown(currentVideoId)
                        },
                    )
                }
            } else if (!feedState.isCardLayoutEnabled) {
                Game(
                    feedDetails = feedState.feedDetails[pageNo],
                    pageNo = pageNo,
                    gameViewModel = gameViewModel,
                    onboardingNudgeType = onboardingNudgeType,
                    onOnboardingNudgeComplete = { feedViewModel.dismissOnboardingStep() },
                )
            }

            if (!feedState.isCardLayoutEnabled &&
                (gameState.isAutoScrollEnabled || feedState.currentOnboardingStep != null)
            ) {
                var resultOfCurrentPage by remember { mutableStateOf<VoteResult?>(null) }
                val voteResult = gameState.gameResult[currentVideoId]?.second

                LaunchedEffect(voteResult?.coinDelta, pageNo) {
                    if (voteResult != null && pageNo == feedState.currentPageOfFeed) {
                        resultOfCurrentPage = voteResult
                    }
                }

                LaunchedEffect(
                    resultOfCurrentPage?.coinDelta,
                    gameState.showResultSheet,
                    pageNo,
                ) {
                    val resultCoinDelta = resultOfCurrentPage?.coinDelta ?: 0
                    val resultHasShownAnimation = resultOfCurrentPage?.hasShownAnimation ?: false
                    val isCurrentPage = pageNo == feedState.currentPageOfFeed
                    val isResultSheetShown = gameState.showResultSheet && resultCoinDelta != 0 && isCurrentPage
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
