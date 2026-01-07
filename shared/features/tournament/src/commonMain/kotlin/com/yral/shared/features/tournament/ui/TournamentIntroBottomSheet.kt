package com.yral.shared.features.tournament.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yral.shared.libs.designsystem.component.YralBottomSheet
import com.yral.shared.libs.designsystem.component.YralButtonType
import com.yral.shared.libs.designsystem.component.YralGradientButton
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.libs.designsystem.theme.appTypoGraphy
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import yral_mobile.shared.features.tournament.generated.resources.Res
import yral_mobile.shared.features.tournament.generated.resources.tournament_intro_background
import yral_mobile.shared.features.tournament.generated.resources.tournament_intro_description
import yral_mobile.shared.features.tournament.generated.resources.tournament_intro_introducing
import yral_mobile.shared.features.tournament.generated.resources.tournament_intro_reward
import yral_mobile.shared.features.tournament.generated.resources.tournament_intro_smiley_game
import yral_mobile.shared.features.tournament.generated.resources.tournament_intro_view_button

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentIntroBottomSheet(
    onDismissRequest: () -> Unit,
    onViewTournamentsClick: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    YralBottomSheet(
        onDismissRequest = onDismissRequest,
        bottomSheetState = sheetState,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .paint(
                        painter = painterResource(Res.drawable.tournament_intro_background),
                        contentScale = ContentScale.FillBounds,
                    ).padding(horizontal = 16.dp, vertical = 24.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // "INTRODUCING" text
                Text(
                    text = stringResource(Res.string.tournament_intro_introducing),
                    style = LocalAppTopography.current.mdBold,
                    color = YralColors.Neutral0,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // "SMILY GAME" image title
                Image(
                    painter = painterResource(Res.drawable.tournament_intro_smiley_game),
                    contentDescription = "Smiley Game",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // "Win upto ₹10,000 worth of Bitcoin" text
                Text(
                    text = stringResource(Res.string.tournament_intro_reward),
                    style = LocalAppTopography.current.mdBold,
                    color = YralColors.Yellow200,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(8.dp))

                // "Play tournaments, rank up, and earn rewards." text
                Text(
                    text = stringResource(Res.string.tournament_intro_description),
                    style = LocalAppTopography.current.mdMedium,
                    color = YralColors.Neutral0,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(24.dp))

                // "View Tournaments" button with white background and pink gradient text
                YralGradientButton(
                    text = stringResource(Res.string.tournament_intro_view_button),
                    buttonType = YralButtonType.White,
                    onClick = onViewTournamentsClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Suppress("UnusedPrivateMember")
@Preview
@Composable
private fun TournamentIntroBottomSheetPreview() {
    CompositionLocalProvider(LocalAppTopography provides appTypoGraphy()) {
        TournamentIntroBottomSheet(
            onDismissRequest = {},
            onViewTournamentsClick = {},
        )
    }
}
