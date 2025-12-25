package com.yral.shared.features.tournament.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yral.shared.libs.designsystem.component.YralBottomSheet
import com.yral.shared.libs.designsystem.component.YralGradientButton
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.libs.designsystem.theme.appTypoGraphy
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import yral_mobile.shared.features.tournament.generated.resources.Res
import yral_mobile.shared.features.tournament.generated.resources.leave_tournament_exit_anyway
import yral_mobile.shared.features.tournament.generated.resources.leave_tournament_keep_playing
import yral_mobile.shared.features.tournament.generated.resources.leave_tournament_message
import yral_mobile.shared.features.tournament.generated.resources.leave_tournament_title
import yral_mobile.shared.features.tournament.generated.resources.tournament_exit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveTournamentBottomSheet(
    tournamentTitle: String,
    totalPrizePool: Int,
    onDismissRequest: () -> Unit,
    onKeepPlayingClick: () -> Unit,
    onExitAnywayClick: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    YralBottomSheet(
        onDismissRequest = onDismissRequest,
        bottomSheetState = sheetState,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(Res.drawable.tournament_exit),
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                contentScale = ContentScale.FillBounds,
            )
            Spacer(modifier = Modifier.height(40.dp))
            Text(
                text = stringResource(Res.string.leave_tournament_title),
                style = LocalAppTopography.current.lgBold,
                color = YralColors.NeutralTextPrimary,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(Res.string.leave_tournament_message, totalPrizePool),
                style = LocalAppTopography.current.mdRegular,
                color = YralColors.Neutral300,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(40.dp))
            YralGradientButton(
                text = stringResource(Res.string.leave_tournament_keep_playing),
                onClick = onKeepPlayingClick,
            )
            Spacer(modifier = Modifier.height(12.dp))
            ExitAnywayButton(onClick = onExitAnywayClick)
        }
    }
}

@Composable
private fun ExitAnywayButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(45.dp)
                .background(YralColors.Neutral800, shape)
                .border(1.dp, YralColors.Neutral700, shape)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(Res.string.leave_tournament_exit_anyway),
            style = LocalAppTopography.current.mdMedium,
            color = YralColors.NeutralTextPrimary,
        )
    }
}

@Suppress("UnusedPrivateMember")
@Preview
@Composable
private fun LeaveTournamentBottomSheetPreview() {
    CompositionLocalProvider(LocalAppTopography provides appTypoGraphy()) {
        LeaveTournamentBottomSheet(
            tournamentTitle = "Smily Showdown",
            totalPrizePool = 10000,
            onDismissRequest = {},
            onKeepPlayingClick = {},
            onExitAnywayClick = {},
        )
    }
}
