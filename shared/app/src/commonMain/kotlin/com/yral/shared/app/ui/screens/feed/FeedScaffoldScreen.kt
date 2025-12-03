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
import com.yral.shared.app.ui.screens.feed.performance.PrefetchVideoListenerImpl
import com.yral.shared.app.ui.screens.feed.performance.VideoListenerImpl
import com.yral.shared.features.feed.nav.FeedComponent
import com.yral.shared.features.feed.ui.FeedScreen
import com.yral.shared.features.feed.ui.components.UserBrief
import com.yral.shared.features.feed.viewmodel.FeedState
import com.yral.shared.features.feed.viewmodel.FeedViewModel
import com.yral.shared.features.feed.viewmodel.OverlayType
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
import yral_mobile.shared.libs.designsystem.generated.resources.shadow
import kotlin.time.Duration.Companion.seconds
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
            )
        },
        bottomOverlay = { pageNo, scrollToNext ->
            OverlayBottom(pageNo, feedState, gameState, gameViewModel, scrollToNext)
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
            gameViewModel.showNudge(
                nudgeIntention = NudgeType.MANDATORY,
                pageNo = pageNo,
                feedDetailsSize = feedState.feedDetails.size,
            )
        },
        limitReelCount =
            if (gameState.isStopAndVote) {
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
    LaunchedEffect(feedState.feedDetails.size, gameState.gameIcons.size) {
        if (feedState.feedDetails.isNotEmpty() &&
            gameState.gameIcons.isNotEmpty() &&
            !gameState.isDefaultMandatoryNudgeShown
        ) {
            gameViewModel.showNudge(
                nudgeIntention = NudgeType.MANDATORY,
                pageNo = feedState.currentPageOfFeed,
                feedDetailsSize = feedState.feedDetails.size,
            )
        }
    }
}

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
) {
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
            )
        }
        OverlayType.GAME_TOGGLE -> {
            OverlayTopGameToggle(
                gameState = gameState,
                setAnimateCoinBalance = setAnimateCoinBalance,
                updateGameType = updateGameType,
            )
        }
        OverlayType.DAILY_RANK ->
            OverlayTopDailyRank(
                dailyRank = dailyRank,
                gameState = gameState,
                setAnimateCoinBalance = setAnimateCoinBalance,
            )
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
                modifier = Modifier.padding(vertical = 22.dp),
            )
        }
    }
}

@Composable
private fun OverlayTopDailyRank(
    dailyRank: Long?,
    gameState: GameState,
    setAnimateCoinBalance: (Boolean) -> Unit,
) {
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
                modifier = Modifier.padding(vertical = 32.dp).align(Alignment.TopStart),
            )
        }
        CoinBalance(
            coinBalance = gameState.coinBalance,
            coinDelta = gameState.lastBalanceDifference,
            animateBag = gameState.animateCoinBalance,
            setAnimate = { setAnimateCoinBalance(it) },
            modifier = Modifier.padding(vertical = 32.dp).align(Alignment.TopEnd),
        )
    }
}

@Composable
private fun OverlayTopGameToggle(
    gameState: GameState,
    setAnimateCoinBalance: (Boolean) -> Unit,
    updateGameType: (GameType) -> Unit,
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
                modifier = Modifier.padding(vertical = 22.dp),
            )
        }
    }
}

data class TopComponentAnimationInfo(
    val isTopRightAnimating: Boolean,
)

@Composable
private fun OverlayBottom(
    pageNo: Int,
    feedState: FeedState,
    gameState: GameState,
    gameViewModel: GameViewModel,
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
            Game(
                feedDetails = feedState.feedDetails[pageNo],
                pageNo = pageNo,
                gameViewModel = gameViewModel,
            )
            if (gameState.isAutoScrollEnabled) {
                val resultOfCurrentPage =
                    gameState.gameResult[feedState.feedDetails[pageNo].videoID]
                val coinDelta = resultOfCurrentPage?.second?.coinDelta ?: 0
                val hasShownAnimation = resultOfCurrentPage?.second?.hasShownAnimation ?: false
                LaunchedEffect(coinDelta, hasShownAnimation, pageNo, gameState.showResultSheet) {
                    val shouldAutoScroll = coinDelta != 0 && !hasShownAnimation && pageNo == feedState.currentPageOfFeed
                    if (shouldAutoScroll && !gameState.showResultSheet) {
                        delay(1.seconds)
                        scrollToNext()
                    }
                }
            }
        }
    }
}
