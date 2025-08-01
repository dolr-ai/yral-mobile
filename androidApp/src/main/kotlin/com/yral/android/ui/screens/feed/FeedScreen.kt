package com.yral.android.ui.screens.feed

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import com.yral.android.R
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.screens.feed.nav.FeedComponent
import com.yral.android.ui.screens.feed.performance.PrefetchVideoListenerImpl
import com.yral.android.ui.screens.feed.performance.VideoListenerImpl
import com.yral.android.ui.screens.feed.uiComponets.GameToggle
import com.yral.android.ui.screens.feed.uiComponets.HowToPlay
import com.yral.android.ui.screens.feed.uiComponets.RefreshBalanceAnimation
import com.yral.android.ui.screens.feed.uiComponets.ReportVideo
import com.yral.android.ui.screens.feed.uiComponets.ReportVideoSheet
import com.yral.android.ui.screens.feed.uiComponets.SignupNudge
import com.yral.android.ui.screens.feed.uiComponets.UserBrief
import com.yral.android.ui.screens.game.AboutGameSheet
import com.yral.android.ui.screens.game.CoinBalance
import com.yral.android.ui.screens.game.GameResultSheet
import com.yral.android.ui.screens.game.SmileyGame
import com.yral.android.ui.widgets.YralAsyncImage
import com.yral.android.ui.widgets.YralErrorMessage
import com.yral.android.ui.widgets.YralLoader
import com.yral.shared.features.feed.viewmodel.FeedState
import com.yral.shared.features.feed.viewmodel.FeedViewModel
import com.yral.shared.features.feed.viewmodel.FeedViewModel.Companion.PRE_FETCH_BEFORE_LAST
import com.yral.shared.features.feed.viewmodel.FeedViewModel.Companion.SIGN_UP_PAGE
import com.yral.shared.features.feed.viewmodel.OverlayType
import com.yral.shared.features.feed.viewmodel.ReportSheetState
import com.yral.shared.features.game.viewmodel.GameState
import com.yral.shared.features.game.viewmodel.GameViewModel
import com.yral.shared.features.game.viewmodel.GameViewModel.Companion.NUDGE_PAGE
import com.yral.shared.libs.videoPlayer.YRALReelPlayer
import com.yral.shared.libs.videoPlayer.model.Reels
import com.yral.shared.rust.domain.models.FeedDetails
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod", "CyclomaticComplexMethod", "UnusedParameter")
@Composable
fun FeedScreen(
    component: FeedComponent,
    modifier: Modifier = Modifier,
    viewModel: FeedViewModel = koinViewModel(),
    gameViewModel: GameViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val gameState by gameViewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.pushScreenView() }

    // Set initial video ID when feed loads
    LaunchedEffect(state.feedDetails.isNotEmpty()) {
        if (state.currentPageOfFeed < state.feedDetails.size) {
            gameViewModel.setCurrentVideoId(state.feedDetails[state.currentPageOfFeed].videoID)
        }
    }

    // Pagination logic
    val isNearEnd by remember {
        derivedStateOf {
            state.feedDetails.isNotEmpty() &&
                (state.feedDetails.size - state.currentPageOfFeed) <= PRE_FETCH_BEFORE_LAST
        }
    }
    LaunchedEffect(isNearEnd, state.isLoadingMore, state.pendingFetchDetails) {
        if (isNearEnd && !state.isLoadingMore && state.pendingFetchDetails == 0) {
            Logger.d("FeedPagination") { "triggering pagination" }
            viewModel.loadMoreFeed()
        }
    }

    // Determine if we should show the loader
    val showLoader =
        isNearEnd &&
            (state.isLoadingMore || state.pendingFetchDetails > 0) &&
            state.currentPageOfFeed == state.feedDetails.size - 1

    Column(modifier = modifier) {
        if (state.feedDetails.isNotEmpty()) {
            YRALReelPlayer(
                modifier = Modifier.weight(1f),
                reels = getReels(state),
                initialPage = state.currentPageOfFeed,
                onPageLoaded = { page ->
                    // Mark animation as shown for the previous page when changing pages
                    if (viewModel.shouldMarkAnimationAsCompleted(page)) {
                        gameViewModel.markCoinDeltaAnimationShown(
                            videoId = state.feedDetails[state.currentPageOfFeed].videoID,
                        )
                    }
                    // Set current video ID for the new page
                    if (page < state.feedDetails.size) {
                        gameViewModel.setCurrentVideoId(state.feedDetails[page].videoID)
                    }
                    viewModel.onCurrentPageChange(page)
                    viewModel.setPostDescriptionExpanded(false)
                },
                recordTime = { currentTime, totalTime ->
                    viewModel.recordTime(currentTime, totalTime)
                },
                didVideoEnd = { viewModel.didCurrentVideoEnd() },
                getPrefetchListener = { PrefetchVideoListenerImpl(it) },
                getVideoListener = { reel ->
                    VideoListenerImpl(
                        reel = reel,
                        registerTrace = { id, tractType ->
                            viewModel.registerTrace(id, tractType.name)
                        },
                        isTraced = { id, traceType ->
                            viewModel.isAlreadyTraced(id, traceType.name)
                        },
                    )
                },
            ) { pageNo ->
                FeedOverlay(
                    pageNo = pageNo,
                    state = state,
                    feedViewModel = viewModel,
                    gameState = gameState,
                    gameViewModel = gameViewModel,
                )
            }
            // Show loader at the bottom when loading more content AND no new items have been added yet
            if (showLoader) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    YralLoader(size = 20.dp)
                }
            }
        }
    }
    if (state.reportSheetState is ReportSheetState.Open) {
        ReportVideoSheet(
            bottomSheetState =
                rememberModalBottomSheetState(
                    skipPartiallyExpanded = true,
                ),
            onDismissRequest = { viewModel.toggleReportSheet(false, 0) },
            isLoading = state.isLoading,
            reasons = (state.reportSheetState as ReportSheetState.Open).reasons,
            onSubmit = { reason, text ->
                viewModel.reportVideo(
                    reason = reason,
                    text = text,
                    pageNo = (state.reportSheetState as ReportSheetState.Open).pageNo,
                )
            },
        )
    }
    if (gameState.showResultSheet && state.currentPageOfFeed < state.feedDetails.size) {
        val currentVideoId = state.feedDetails[state.currentPageOfFeed].videoID
        val coinDelta = gameViewModel.getFeedGameResult(currentVideoId)
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
    }
    if (gameState.showAboutGame && state.currentPageOfFeed < state.feedDetails.size) {
        AboutGameSheet(
            gameRules = gameState.gameRules,
            onDismissRequest = {
                gameViewModel.toggleAboutGame(false)
            },
        )
    }
    if (state.showSignupFailedSheet) {
        YralErrorMessage(
            title = stringResource(R.string.could_not_login),
            error = stringResource(R.string.could_not_login_desc),
            sheetState =
                rememberModalBottomSheetState(
                    skipPartiallyExpanded = true,
                ),
            cta = stringResource(R.string.ok),
            onClick = { viewModel.toggleSignupFailed(false) },
            onDismiss = { viewModel.toggleSignupFailed(false) },
        )
    }
}

private fun getReels(state: FeedState): List<Reels> =
    state
        .feedDetails
        .map { it.toReel() }
        .toList()

private fun FeedDetails.toReel() =
    Reels(
        videoUrl = url.toString(),
        thumbnailUrl = thumbnail.toString(),
        videoId = videoID,
    )

@Composable
private fun FeedOverlay(
    pageNo: Int,
    state: FeedState,
    feedViewModel: FeedViewModel,
    gameState: GameState,
    gameViewModel: GameViewModel,
) {
    var lottieCached by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopStart,
    ) {
        when (state.overlayType) {
            OverlayType.DEFAULT -> {
                TopView(
                    state = state,
                    pageNo = pageNo,
                    feedViewModel = feedViewModel,
                    gameState = gameState,
                    gameViewModel = gameViewModel,
                )
            }
            OverlayType.GAME_TOGGLE -> {
                TopViewWithGameToggle(
                    gameState = gameState,
                    gameViewModel = gameViewModel,
                )
            }
        }
        BottomView(
            state = state,
            pageNo = pageNo,
            feedViewModel = feedViewModel,
            gameState = gameState,
            gameViewModel = gameViewModel,
        )
        if (!lottieCached) {
            gameState.gameIcons.forEach { icon ->
                // Enable if want to use remote lottie
                // PreloadLottieAnimation(icon.clickAnimation)
            }
            lottieCached = true
        }
        if (!feedViewModel.isLoggedIn() && pageNo != 0 && (pageNo % SIGN_UP_PAGE) == 0) {
            SignupNudge {
                feedViewModel.signInWithGoogle()
            }
        }
    }
}

@Composable
private fun TopViewWithGameToggle(
    gameState: GameState,
    gameViewModel: GameViewModel,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .paint(
                    painter = painterResource(R.drawable.shadow),
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
        ) { gameViewModel.updateGameType(it) }
        Row(
            modifier =
                Modifier
                    .padding(start = 26.dp, end = 26.dp, top = 11.dp)
                    .align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_menu),
                contentDescription = "image description",
                contentScale = ContentScale.None,
                modifier = Modifier.size(32.dp),
            )
            Spacer(modifier = Modifier.weight(1f))
            CoinBalance(
                coinBalance = gameState.coinBalance,
                coinDelta = gameState.lastBalanceDifference,
                animateBag = gameState.animateCoinBalance,
                setAnimate = { gameViewModel.setAnimateCoinBalance(it) },
                modifier = Modifier.padding(vertical = 22.dp),
            )
        }
    }
}

@Composable
private fun TopView(
    state: FeedState,
    pageNo: Int,
    feedViewModel: FeedViewModel,
    gameState: GameState,
    gameViewModel: GameViewModel,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .paint(
                    painter = painterResource(R.drawable.shadow),
                    contentScale = ContentScale.FillBounds,
                ).padding(end = 26.dp),
    ) {
        var paddingEnd by remember { mutableFloatStateOf(0f) }
        val density = LocalDensity.current
        val screenWidthPx = LocalWindowInfo.current.containerSize.width
        UserBrief(
            principalId = state.feedDetails[pageNo].principalID,
            profileImageUrl = state.feedDetails[pageNo].profileImageURL,
            postDescription = state.feedDetails[pageNo].postDescription,
            isPostDescriptionExpanded = state.isPostDescriptionExpanded,
            setPostDescriptionExpanded = { feedViewModel.setPostDescriptionExpanded(it) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(end = with(density) { paddingEnd.toDp() + 46.dp }),
        )
        CoinBalance(
            coinBalance = gameState.coinBalance,
            coinDelta = gameState.lastBalanceDifference,
            animateBag = gameState.animateCoinBalance,
            setAnimate = { gameViewModel.setAnimateCoinBalance(it) },
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(vertical = 32.dp)
                    .onGloballyPositioned { coordinates ->
                        if (!gameState.animateCoinBalance) {
                            val x = coordinates.positionInParent().x
                            paddingEnd = screenWidthPx - x
                        }
                    },
        )
    }
}

@Composable
private fun BottomView(
    state: FeedState,
    pageNo: Int,
    feedViewModel: FeedViewModel,
    gameState: GameState,
    gameViewModel: GameViewModel,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Shadow(Modifier.align(Alignment.BottomCenter))
        HowToPlay(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 105.dp),
            shouldExpand =
                pageNo < gameState.isHowToPlayShown.size &&
                    !gameState.isHowToPlayShown[pageNo] &&
                    pageNo == state.currentPageOfFeed,
            onClick = { gameViewModel.toggleAboutGame(true) },
            onAnimationComplete = { gameViewModel.setHowToPlayShown(pageNo, state.currentPageOfFeed) },
        )
        ActionsRight(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 105.dp),
            pageNo = pageNo,
            state = state,
            feedViewModel = feedViewModel,
        )
        Game(state, pageNo, gameState, gameViewModel)
        RefreshBalanceAnimation(
            refreshBalanceState = gameState.refreshBalanceState,
            onAnimationComplete = { gameViewModel.hideRefreshBalanceAnimation() },
        )
    }
}

@Composable
private fun Shadow(modifier: Modifier) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .paint(
                    painter = painterResource(R.drawable.shadow_bottom),
                    contentScale = ContentScale.FillBounds,
                    alpha = 0.3f,
                ),
        content = { },
    )
}

@Composable
private fun ActionsRight(
    modifier: Modifier,
    pageNo: Int,
    state: FeedState,
    feedViewModel: FeedViewModel,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(26.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state.overlayType == OverlayType.GAME_TOGGLE) {
            state.feedDetails[pageNo].profileImageURL?.let { profileImage ->
                YralAsyncImage(
                    imageUrl = profileImage.toString(),
                    modifier = Modifier.size(36.dp),
                    border = 2.dp,
                    borderColor = Color.White,
                    backgroundColor = YralColors.ProfilePicBackground,
                )
            }
        }
        ReportVideo(
            onReportClicked = { feedViewModel.toggleReportSheet(true, pageNo) },
        )
    }
}

@Composable
private fun Game(
    state: FeedState,
    pageNo: Int,
    gameState: GameState,
    gameViewModel: GameViewModel,
) {
    if (gameState.gameIcons.isNotEmpty()) {
        SmileyGame(
            gameIcons = gameState.gameIcons,
            clickedIcon = gameState.gameResult[state.feedDetails[pageNo].videoID]?.first,
            onIconClicked = { icon, isTutorialVote ->
                gameViewModel.setClickedIcon(
                    icon = icon,
                    feedDetails = state.feedDetails[pageNo],
                    isTutorialVote = isTutorialVote,
                )
            },
            coinDelta = gameViewModel.getFeedGameResult(state.feedDetails[pageNo].videoID),
            errorMessage = gameViewModel.getFeedGameResultError(state.feedDetails[pageNo].videoID),
            isLoading = gameState.isLoading,
            hasShownCoinDeltaAnimation =
                gameViewModel.hasShownCoinDeltaAnimation(
                    videoId = state.feedDetails[pageNo].videoID,
                ),
            onDeltaAnimationComplete = {
                gameViewModel.markCoinDeltaAnimationShown(
                    videoId = state.feedDetails[pageNo].videoID,
                )
            },
            shouldShowNudge =
                !gameState.isSmileyGameNudgeShown &&
                    pageNo >= NUDGE_PAGE &&
                    pageNo == state.currentPageOfFeed,
            onNudgeAnimationComplete = {
                gameViewModel.setSmileyGameNudgeShown(state.feedDetails[pageNo])
            },
        )
    }
}
