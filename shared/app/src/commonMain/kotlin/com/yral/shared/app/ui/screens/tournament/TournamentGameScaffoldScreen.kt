package com.yral.shared.app.ui.screens.tournament

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yral.shared.features.feed.ui.FeedScreen
import com.yral.shared.features.feed.viewmodel.FeedContext
import com.yral.shared.features.feed.viewmodel.FeedViewModel
import com.yral.shared.features.tournament.nav.TournamentGameComponent
import com.yral.shared.features.tournament.ui.LeaveTournamentBottomSheet
import com.yral.shared.features.tournament.ui.OutOfDiamondsBottomSheet
import com.yral.shared.features.tournament.ui.PlayType
import com.yral.shared.features.tournament.ui.TournamentBottomOverlay
import com.yral.shared.features.tournament.ui.TournamentGameActionsRight
import com.yral.shared.features.tournament.ui.TournamentHowToPlayScreen
import com.yral.shared.features.tournament.ui.TournamentTopOverlay
import com.yral.shared.features.tournament.viewmodel.TournamentGameViewModel
import com.yral.shared.libs.designsystem.component.lottie.PreloadLottieAnimations
import com.yral.shared.libs.videoPlayer.cardstack.SwipeDirection
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Suppress("LongMethod", "MagicNumber", "CyclomaticComplexMethod")
@OptIn(ExperimentalTime::class, ExperimentalComposeUiApi::class)
@Composable
fun TournamentGameScaffoldScreen(
    component: TournamentGameComponent,
    sessionKey: String,
) {
    val gameConfig = component.gameConfig
    val tournamentGameViewModel =
        koinViewModel<TournamentGameViewModel>(
            key = "tournament-game-${gameConfig.tournamentId}-$sessionKey",
        )
    val tournamentFeedViewModel =
        koinViewModel<FeedViewModel>(
            key = "tournament-feed-${gameConfig.tournamentId}-$sessionKey",
            parameters = {
                parametersOf(
                    FeedContext.Tournament(
                        tournamentId = gameConfig.tournamentId,
                        sessionKey = sessionKey,
                        isHotOrNot = gameConfig.isHotOrNot,
                    ),
                )
            },
        )
    val gameState by tournamentGameViewModel.state.collectAsStateWithLifecycle()
    val feedState by tournamentFeedViewModel.state.collectAsStateWithLifecycle()
    var timeLeftMs by remember(gameConfig.endEpochMs) {
        mutableLongStateOf(
            maxOf(0L, gameConfig.endEpochMs - Clock.System.now().toEpochMilliseconds()),
        )
    }
    // Show how-to-play only if user hasn't played yet in this tournament (from API)
    // Start with true, then hide once API confirms user has played before
    var showHowToPlay by remember(gameConfig.tournamentId) { mutableStateOf(true) }
    var howToPlayOpenedFromButton by remember(gameConfig.tournamentId) { mutableStateOf(false) }

    // Auto-hide how-to-play if API says user has played before
    LaunchedEffect(gameState.hasPlayedBefore) {
        if (gameState.hasPlayedBefore && !howToPlayOpenedFromButton) {
            showHowToPlay = false
        }
    }
    var showLeaveTournamentConfirmation by remember(gameConfig.tournamentId) {
        mutableStateOf(false)
    }

    LaunchedEffect(gameConfig.endEpochMs) {
        while (timeLeftMs > 0) {
            @Suppress("MagicNumber")
            delay(1000L)
            timeLeftMs = maxOf(0L, gameConfig.endEpochMs - Clock.System.now().toEpochMilliseconds())
        }
        if (timeLeftMs <= 0 && gameConfig.endEpochMs > 0) {
            tournamentGameViewModel.trackTournamentEnded(gameConfig.tournamentTitle)
            component.onTimeUp()
        }
    }

    // Initialize the game view model with tournament data
    LaunchedEffect(gameConfig) {
        val tournamentType =
            if (gameConfig.isHotOrNot) {
                com.yral.shared.features.tournament.domain.model.TournamentType.HOT_OR_NOT
            } else {
                com.yral.shared.features.tournament.domain.model.TournamentType.SMILEY
            }
        tournamentGameViewModel.setTournament(
            tournamentId = gameConfig.tournamentId,
            tournamentType = tournamentType,
            initialDiamonds = gameConfig.initialDiamonds,
            endEpochMs = gameConfig.endEpochMs,
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            FeedScreen(
                component = component,
                viewModel = tournamentFeedViewModel,
                topOverlay = { _ ->
                    TournamentTopOverlay(
                        gameState = gameState,
                        tournamentTitle = gameConfig.tournamentTitle,
                        onLeaderboardClick = { /*component.onLeaderboardClick()*/ },
                        onBack = {
                            tournamentGameViewModel.trackExitAttempted()
                            showLeaveTournamentConfirmation = true
                        },
                    )
                },
                bottomOverlay = { pageNo, _ ->
                    if (pageNo < feedState.feedDetails.size) {
                        val totalDurationMs = gameConfig.endEpochMs - gameConfig.startEpochMs
                        TournamentBottomOverlay(
                            pageNo = pageNo,
                            feedDetails = feedState.feedDetails[pageNo],
                            gameState = gameState,
                            gameViewModel = tournamentGameViewModel,
                            timeLeftMs = timeLeftMs,
                            totalDurationMs = totalDurationMs,
                            isHotOrNot = gameConfig.isHotOrNot,
                        )
                    }
                },
                actionsRight = { pageNo ->
                    TournamentGameActionsRight(
                        onHowToPlayClick = {
                            howToPlayOpenedFromButton = true
                            showHowToPlay = true
                        },
                        onExit = {
                            tournamentGameViewModel.trackExitAttempted()
                            showLeaveTournamentConfirmation = true
                        },
                        onReport = { tournamentFeedViewModel.toggleReportSheet(true, pageNo) },
                        onHowToPlay = {
                            howToPlayOpenedFromButton = true
                            showHowToPlay = true
                        },
                    )
                },
                onPageChanged = { pageNo, _ ->
                    if (pageNo >= 0 && pageNo < feedState.feedDetails.size) {
                        tournamentGameViewModel.setCurrentVideoId(
                            feedState.feedDetails[pageNo].videoID,
                        )
                    }
                },
                onEdgeScrollAttempt = { _ -> },
                limitReelCount = feedState.feedDetails.size,
                onSwipeVote = { direction, pageIndex ->
                    // Hot or Not voting: right swipe = hot, left swipe = not
                    val videoId = feedState.feedDetails.getOrNull(pageIndex)?.videoID
                    if (videoId != null) {
                        val isHot = direction == SwipeDirection.RIGHT
                        tournamentGameViewModel.castSwipeVote(isHot, videoId)
                    }
                },
            )
            BackHandler(onBack = { showLeaveTournamentConfirmation = true })
            if (gameState.gameIcons.isNotEmpty()) {
                PreloadLottieAnimations(
                    urls = gameState.gameIcons.map { it.clickAnimation },
                )
            }

            if (showHowToPlay) {
                val tournamentDurationMinutes = ((gameConfig.endEpochMs - gameConfig.startEpochMs) / 60_000).toInt()
                TournamentHowToPlayScreen(
                    title = gameConfig.tournamentTitle,
                    onStartPlaying = { showHowToPlay = false },
                    startingDiamonds = component.gameConfig.initialDiamonds,
                    playType = if (howToPlayOpenedFromButton) PlayType.CONTINUE else PlayType.START,
                    tournamentDurationMinutes = tournamentDurationMinutes,
                    isHotOrNot = gameConfig.isHotOrNot,
                )
            }

            // Show no diamonds bottom sheet
            if (gameState.noDiamondsError) {
                OutOfDiamondsBottomSheet(
                    onDismissRequest = { tournamentGameViewModel.clearNoDiamondsError() },
                    onViewTournamentsClick = {
                        tournamentGameViewModel.clearNoDiamondsError()
                        component.onBack()
                    },
                    onExitAnywayClick = {
                        tournamentGameViewModel.clearNoDiamondsError()
                        component.onBack()
                    },
                )
            }

            // Navigate to leaderboard to show results when tournament ends
            if (gameState.tournamentEndedError) {
                LaunchedEffect(gameState.tournamentEndedError) {
                    tournamentGameViewModel.clearTournamentEndedError()
                    component.onTimeUp()
                }
            }

            if (showLeaveTournamentConfirmation) {
                // Track that the exit nudge is shown
                LaunchedEffect(Unit) {
                    tournamentGameViewModel.trackExitNudgeShown()
                }
                LeaveTournamentBottomSheet(
                    onDismissRequest = { showLeaveTournamentConfirmation = false },
                    onKeepPlayingClick = { showLeaveTournamentConfirmation = false },
                    totalPrizePool = component.gameConfig.totalPrizePool,
                    onExitAnywayClick = {
                        tournamentGameViewModel.trackExitConfirmed()
                        showLeaveTournamentConfirmation = false
                        component.onBack()
                    },
                )
            }
        }
    }
}
