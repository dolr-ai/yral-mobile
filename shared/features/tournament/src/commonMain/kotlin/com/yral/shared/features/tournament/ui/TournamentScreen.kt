package com.yral.shared.features.tournament.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yral.shared.core.session.SessionManager
import com.yral.shared.features.tournament.nav.TournamentComponent
import com.yral.shared.features.tournament.viewmodel.TournamentUiState
import com.yral.shared.features.tournament.viewmodel.TournamentViewModel
import com.yral.shared.libs.designsystem.component.YralGradientButton
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.libs.designsystem.theme.appTypoGraphy
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import yral_mobile.shared.features.tournament.generated.resources.Res
import yral_mobile.shared.features.tournament.generated.resources.join_tournament
import yral_mobile.shared.features.tournament.generated.resources.login_and_join_tournament
import yral_mobile.shared.features.tournament.generated.resources.msg_join_tournament
import yral_mobile.shared.features.tournament.generated.resources.msg_login_to_join_tournament
import yral_mobile.shared.features.tournament.generated.resources.no_tournament_history_yet
import yral_mobile.shared.features.tournament.generated.resources.screen_heading
import yral_mobile.shared.features.tournament.generated.resources.tab_all
import yral_mobile.shared.features.tournament.generated.resources.tab_history

@Suppress("LongMethod")
@Composable
fun TournamentScreen(
    component: TournamentComponent,
    viewModel: TournamentViewModel,
) {
    val uiState by viewModel.state.collectAsState()

    LaunchedEffect(key1 = Unit) {
        viewModel.eventsFlow.collectLatest { value ->
            component.processEvent(value)
        }
    }

    val showHowToPlayTournament = uiState.showHowToPlayTournament
    if (showHowToPlayTournament != null) {
        TournamentHowToPlayScreen(
            title = showHowToPlayTournament.title,
            onStartPlaying = viewModel::onStartPlayingClicked,
        )
    } else {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(YralColors.Neutral950),
        ) {
            Text(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                text = stringResource(Res.string.screen_heading),
                textAlign = TextAlign.Center,
                style = LocalAppTopography.current.xlBold,
                color = YralColors.NeutralTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            TournamentTabs(
                selectedTab = uiState.selectedTab,
                onTabSelected = viewModel::onTabSelected,
            )

            if (uiState.selectedTab == TournamentUiState.Tab.History && uiState.tournaments.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    NoTournamentHistory(
                        isLoggedIn = uiState.isLoggedIn,
                        onClick = viewModel::onNoHistoryCtaClicked,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(uiState.tournaments) { tournament ->
                        TournamentCard(
                            tournament = tournament,
                            onPrizeBreakdownClick = { viewModel.openPrizeBreakdown(tournament) },
                            onShareClick = { viewModel.onShareClicked(tournament) },
                            onTournamentCtaClick = { viewModel.onTournamentCtaClick(tournament) },
                        )
                    }
                }
            }
        }
    }

    val selected = uiState.prizeBreakdownTournament
    if (selected != null) {
        PrizeBreakdownBottomSheet(
            rows = selected.prizeBreakdown,
            onDismissRequest = viewModel::closePrizeBreakdown,
            status = selected.status,
            participationState = selected.participationState,
            onCtaClicked = { viewModel.onTournamentCtaClick(selected) },
        )
    }
}

@Composable
private fun TournamentTabs(
    selectedTab: TournamentUiState.Tab,
    onTabSelected: (TournamentUiState.Tab) -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        color = YralColors.Neutral900,
        shape = RoundedCornerShape(999.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, YralColors.Neutral700),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            SegmentedTab(
                modifier = Modifier.weight(1f),
                isSelected = selectedTab == TournamentUiState.Tab.All,
                text = stringResource(Res.string.tab_all),
                onClick = { onTabSelected(TournamentUiState.Tab.All) },
            )
            SegmentedTab(
                modifier = Modifier.weight(1f),
                isSelected = selectedTab == TournamentUiState.Tab.History,
                text = stringResource(Res.string.tab_history),
                onClick = { onTabSelected(TournamentUiState.Tab.History) },
            )
        }
    }
}

@Composable
private fun SegmentedTab(
    isSelected: Boolean,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = if (isSelected) YralColors.Neutral50 else Color.Transparent
    val contentColor = if (isSelected) YralColors.Neutral950 else YralColors.NeutralTextSecondary

    Box(
        modifier =
            modifier
                .height(36.dp)
                .background(background, RoundedCornerShape(999.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = LocalAppTopography.current.baseBold,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun NoTournamentHistory(
    isLoggedIn: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 36.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.no_tournament_history_yet),
            style = LocalAppTopography.current.xlSemiBold,
            color = YralColors.Neutral0,
            textAlign = TextAlign.Center,
        )
        Text(
            text =
                stringResource(
                    if (isLoggedIn) {
                        Res.string.msg_join_tournament
                    } else {
                        Res.string.msg_login_to_join_tournament
                    },
                ),
            style = LocalAppTopography.current.baseRegular,
            color = YralColors.Neutral0,
            textAlign = TextAlign.Center,
        )
        YralGradientButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(if (isLoggedIn) Res.string.join_tournament else Res.string.login_and_join_tournament),
            onClick = onClick,
        )
    }
}

@Suppress("UnusedPrivateMember")
@Preview
@Composable
private fun TournamentScreenPreview() {
    val component =
        object : TournamentComponent {
            override fun processEvent(value: TournamentViewModel.Event) {
                // no-op
            }
        }
    CompositionLocalProvider(LocalAppTopography provides appTypoGraphy()) {
        TournamentScreen(component = component, viewModel = TournamentViewModel(SessionManager()))
    }
}
