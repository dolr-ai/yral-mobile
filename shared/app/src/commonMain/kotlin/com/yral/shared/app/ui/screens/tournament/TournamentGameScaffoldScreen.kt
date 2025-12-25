package com.yral.shared.app.ui.screens.tournament

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yral.shared.app.ui.screens.feed.performance.PrefetchVideoListenerImpl
import com.yral.shared.features.feed.ui.FeedScreen
import com.yral.shared.features.feed.viewmodel.FeedViewModel
import com.yral.shared.features.tournament.nav.TournamentGameComponent
import com.yral.shared.features.tournament.ui.NoDiamondsDialog
import com.yral.shared.features.tournament.ui.PlayType
import com.yral.shared.features.tournament.ui.TournamentBottomOverlay
import com.yral.shared.features.tournament.ui.TournamentEndedDialog
import com.yral.shared.features.tournament.ui.TournamentGameActionsRight
import com.yral.shared.features.tournament.ui.TournamentHowToPlayScreen
import com.yral.shared.features.tournament.ui.TournamentTopOverlay
import com.yral.shared.features.tournament.viewmodel.TournamentGameViewModel
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
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

    LaunchedEffect(gameConfig.endEpochMs) {
        while (timeLeftMs > 0) {
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

    FeedScreen(
        component = component,
        viewModel = tournamentFeedViewModel,
        topOverlay = { _ ->
            TournamentTopOverlay(
                gameState = gameState,
                tournamentTitle = gameConfig.tournamentTitle,
                onLeaderboardClick = { component.onLeaderboardClick() },
                onBack = { component.onBack() },
            )
        },
        bottomOverlay = { pageNo, scrollToNext ->
            if (pageNo < feedState.feedDetails.size) {
                TournamentBottomOverlay(
                    feedDetails = feedState.feedDetails[pageNo],
                    gameState = gameState,
                    gameViewModel = tournamentGameViewModel,
                    timeLeftMs = timeLeftMs,
                    onHowToPlayClick = {
                        howToPlayOpenedFromButton = true
                        showHowToPlay = true
                    },
                    scrollToNext = scrollToNext,
                )
            }
        },
        actionsRight = { pageNo ->
            TournamentGameActionsRight(
                onExit = { component.onBack() },
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
        limitReelCount = gameState.lastVotedCount,
        getPrefetchListener = { reel -> PrefetchVideoListenerImpl(reel) },
        getVideoListener = { null },
    )

    if (showHowToPlay) {
        TournamentHowToPlayScreen(
            title = gameConfig.tournamentTitle,
            onStartPlaying = { showHowToPlay = false },
            startingDiamonds = component.gameConfig.initialDiamonds,
            playType = if (howToPlayOpenedFromButton) PlayType.CONTINUE else PlayType.START,
        )
    }

    // Show no diamonds dialog
    if (gameState.noDiamondsError) {
        NoDiamondsDialog(
            onDismiss = { tournamentGameViewModel.clearNoDiamondsError() },
            onExit = { component.onTimeUp() },
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
}
