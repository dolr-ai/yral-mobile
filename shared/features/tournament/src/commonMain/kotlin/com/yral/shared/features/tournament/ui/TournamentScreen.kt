package com.yral.shared.features.tournament.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yral.shared.features.tournament.viewmodel.TournamentUiState
import com.yral.shared.features.tournament.viewmodel.TournamentViewModel
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.libs.designsystem.theme.appTypoGraphy
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import yral_mobile.shared.features.tournament.generated.resources.Res
import yral_mobile.shared.features.tournament.generated.resources.screen_heading
import yral_mobile.shared.features.tournament.generated.resources.tab_all
import yral_mobile.shared.features.tournament.generated.resources.tab_history

@Composable
fun TournamentScreen(viewModel: TournamentViewModel) {
    val uiState by viewModel.state.collectAsState()

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

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                androidx.compose.foundation.layout
                    .PaddingValues(16.dp),
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

@Suppress("UnusedPrivateMember")
@Preview
@Composable
private fun TournamentScreenPreview() {
    CompositionLocalProvider(LocalAppTopography provides appTypoGraphy()) {
        TournamentScreen(viewModel = TournamentViewModel())
    }
}
