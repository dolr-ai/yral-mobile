package com.yral.shared.features.tournament.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yral.shared.libs.designsystem.component.YralButtonType
import com.yral.shared.libs.designsystem.component.YralGradientButton
import com.yral.shared.libs.designsystem.theme.GradientLengthMode
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.libs.designsystem.theme.angledGradientBackground
import com.yral.shared.libs.designsystem.theme.appTypoGraphy
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import yral_mobile.shared.features.tournament.generated.resources.Res
import yral_mobile.shared.features.tournament.generated.resources.tournament_fail_body
import yral_mobile.shared.features.tournament.generated.resources.tournament_fail_emoji
import yral_mobile.shared.features.tournament.generated.resources.tournament_fail_title
import yral_mobile.shared.features.tournament.generated.resources.view_leaderboard
import yral_mobile.shared.libs.designsystem.generated.resources.cross
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@Suppress("LongMethod", "MagicNumber")
@Composable
fun TournamentFailScreen(
    rank: Int,
    nextTournamentTime: String,
    onClose: () -> Unit,
    onViewLeaderboard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .angledGradientBackground(
                    colorStops =
                        arrayOf(
                            0f to YralColors.Neutral700,
                            0.4f to YralColors.Neutral950,
                        ),
                    degrees = -45f,
                    lengthMode = GradientLengthMode.Diagonal,
                ),
    ) {
        Icon(
            painter = painterResource(DesignRes.drawable.cross),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier =
                Modifier
                    .padding(top = 24.dp, end = 24.dp)
                    .size(24.dp)
                    .align(Alignment.TopEnd)
                    .clickable(onClick = onClose),
        )
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(50.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Image(
                    painter = painterResource(Res.drawable.tournament_fail_emoji),
                    contentDescription = null,
                    modifier = Modifier.size(140.dp),
                    contentScale = ContentScale.Fit,
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(Res.string.tournament_fail_title),
                        style =
                            LocalAppTopography.current.lgBold.copy(
                                fontSize = 20.sp,
                                lineHeight = 28.sp,
                            ),
                        color = YralColors.NeutralTextPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text =
                            stringResource(
                                Res.string.tournament_fail_body,
                                rank,
                                nextTournamentTime,
                            ),
                        style = LocalAppTopography.current.mdRegular,
                        color = YralColors.Neutral300,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                ViewLeaderboardButton(onClick = onViewLeaderboard)
            }
        }
    }
}

@Composable
private fun ViewLeaderboardButton(onClick: () -> Unit) {
    YralGradientButton(
        buttonType = YralButtonType.White,
        text = stringResource(Res.string.view_leaderboard),
        onClick = onClick,
    )
}

@Suppress("UnusedPrivateMember", "MagicNumber")
@Preview
@Composable
private fun TournamentFailScreenPreview() {
    CompositionLocalProvider(LocalAppTopography provides appTypoGraphy()) {
        TournamentFailScreen(
            rank = 12,
            nextTournamentTime = "6:00 PM",
            onClose = {},
            onViewLeaderboard = {},
        )
    }
}
