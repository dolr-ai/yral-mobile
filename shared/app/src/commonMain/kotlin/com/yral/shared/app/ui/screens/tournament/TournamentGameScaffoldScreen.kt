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
import com.yral.shared.app.ui.screens.feed.performance.PrefetchVideoListenerImpl
import com.yral.shared.features.feed.ui.FeedScreen
import com.yral.shared.features.feed.viewmodel.FeedViewModel
import com.yral.shared.features.tournament.nav.TournamentGameComponent
import com.yral.shared.features.tournament.ui.LeaveTournamentBottomSheet
import com.yral.shared.features.tournament.ui.OutOfDiamondsBottomSheet
import com.yral.shared.features.tournament.ui.PlayType
import com.yral.shared.features.tournament.ui.TournamentBottomOverlay
import com.yral.shared.features.tournament.ui.TournamentEndedDialog
import com.yral.shared.features.tournament.ui.TournamentGameActionsRight
import com.yral.shared.features.tournament.ui.TournamentHowToPlayScreen
import com.yral.shared.features.tournament.ui.TournamentTopOverlay
import com.yral.shared.features.tournament.viewmodel.TournamentGameViewModel
import com.yral.shared.libs.designsystem.component.lottie.PreloadLottieAnimations
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Suppress("LongMethod")
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
        )
    val gameState by tournamentGameViewModel.state.collectAsStateWithLifecycle()
    val feedState by tournamentFeedViewModel.state.collectAsStateWithLifecycle()
    var timeLeftMs by remember(gameConfig.endEpochMs) {
        mutableLongStateOf(
            maxOf(0L, gameConfig.endEpochMs - Clock.System.now().toEpochMilliseconds()),
        )
    }
    var showHowToPlay by remember(gameConfig.tournamentId) {
        mutableStateOf(true)
    }
    var howToPlayOpenedFromButton by remember(gameConfig.tournamentId) {
        mutableStateOf(false)
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
            component.onTimeUp()
        }
    }

    // Initialize the game view model with tournament data
    LaunchedEffect(gameConfig) {
        tournamentGameViewModel.setTournament(
            tournamentId = gameConfig.tournamentId,
            initialDiamonds = gameConfig.initialDiamonds,
            endEpochMs = gameConfig.endEpochMs,
        )
    }


    Scaffold(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        Box(
            modifier = Modifier
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
                        onLeaderboardClick = { component.onLeaderboardClick() },
                        onBack = { showLeaveTournamentConfirmation = true },
                    )
                },
                bottomOverlay = { pageNo, _ ->
                    if (pageNo < feedState.feedDetails.size) {
                        TournamentBottomOverlay(
                            pageNo = pageNo,
                            feedDetails = feedState.feedDetails[pageNo],
                            gameState = gameState,
                            gameViewModel = tournamentGameViewModel,
                            timeLeftMs = timeLeftMs,
                            onHowToPlayClick = {
                                howToPlayOpenedFromButton = true
                                showHowToPlay = true
                            },
                        )
                    }
                },
                actionsRight = { pageNo ->
                    TournamentGameActionsRight(
                        onExit = { showLeaveTournamentConfirmation = true },
                        onReport = { tournamentFeedViewModel.toggleReportSheet(true, pageNo) },
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
                getPrefetchListener = { reel -> PrefetchVideoListenerImpl(reel) },
                getVideoListener = { null },
            )
            BackHandler(onBack = { showLeaveTournamentConfirmation = true })
            if (gameState.gameIcons.isNotEmpty()) {
                PreloadLottieAnimations(
                    urls = gameState.gameIcons.map { it.clickAnimation },
                )
            }

            if (showHowToPlay) {
                TournamentHowToPlayScreen(
                    title = gameConfig.tournamentTitle,
                    onStartPlaying = { showHowToPlay = false },
                    startingDiamonds = component.gameConfig.initialDiamonds,
                    playType = if (howToPlayOpenedFromButton) PlayType.CONTINUE else PlayType.START,
                )
            }

            // Show no diamonds bottom sheet
            if (gameState.noDiamondsError) {
                OutOfDiamondsBottomSheet(
                    onDismissRequest = { tournamentGameViewModel.clearNoDiamondsError() },
                    onViewTournamentsClick = {
                        tournamentGameViewModel.clearNoDiamondsError()
                        component.onTimeUp()
                    },
                    onExitAnywayClick = {
                        tournamentGameViewModel.clearNoDiamondsError()
                        component.onTimeUp()
                    },
                )
            }

            // Show tournament ended dialog
            if (gameState.tournamentEndedError) {
                TournamentEndedDialog(
                    onViewLeaderboard = {
                        tournamentGameViewModel.clearTournamentEndedError()
                        component.onLeaderboardClick()
                    },
                    onExit = {
                        tournamentGameViewModel.clearTournamentEndedError()
                        component.onTimeUp()
                    },
                )
            }

            if (showLeaveTournamentConfirmation) {
                LeaveTournamentBottomSheet(
                    onDismissRequest = { showLeaveTournamentConfirmation = false },
                    onKeepPlayingClick = { showLeaveTournamentConfirmation = false },
                    totalPrizePool = component.gameConfig.totalPrizePool,
                    onExitAnywayClick = {
                        showLeaveTournamentConfirmation = false
                        component.onBack()
                    },
                )
            }
        }
    }
}
