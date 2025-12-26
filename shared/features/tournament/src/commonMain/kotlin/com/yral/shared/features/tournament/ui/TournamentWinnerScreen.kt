package com.yral.shared.features.tournament.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yral.shared.libs.designsystem.component.YralMaskedVectorTextV2
import com.yral.shared.libs.designsystem.component.lottie.LottieRes
import com.yral.shared.libs.designsystem.component.lottie.YralLottieAnimation
import com.yral.shared.libs.designsystem.theme.GradientAngleConvention
import com.yral.shared.libs.designsystem.theme.GradientLengthMode
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.libs.designsystem.theme.appTypoGraphy
import com.yral.shared.libs.designsystem.theme.linearGradientBrush
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import yral_mobile.shared.features.tournament.generated.resources.Res
import yral_mobile.shared.features.tournament.generated.resources.bitcoin
import yral_mobile.shared.features.tournament.generated.resources.confettie
import yral_mobile.shared.features.tournament.generated.resources.tournament_noise_texture
import yral_mobile.shared.features.tournament.generated.resources.view_leaderboard
import yral_mobile.shared.features.tournament.generated.resources.winner_body_prefix
import yral_mobile.shared.features.tournament.generated.resources.winner_body_suffix
import yral_mobile.shared.features.tournament.generated.resources.winner_celebration_message
import yral_mobile.shared.features.tournament.generated.resources.winner_celebration_title
import yral_mobile.shared.features.tournament.generated.resources.winner_rank_format
import yral_mobile.shared.features.tournament.generated.resources.winner_title
import yral_mobile.shared.libs.designsystem.generated.resources.cross
import yral_mobile.shared.libs.designsystem.generated.resources.golden_gradient
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@Suppress("LongMethod")
@Composable
fun TournamentWinnerScreen(
    prizeAmount: String,
    rank: Int,
    onClose: () -> Unit,
    onViewLeaderboard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(YralColors.Neutral950),
    ) {
        Image(
            painter = painterResource(Res.drawable.tournament_noise_texture),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
        Image(
            painter = painterResource(Res.drawable.confettie),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.5f),
        )
        YralLottieAnimation(
            rawRes = LottieRes.COLORFUL_CONFETTI_BRUST,
            iterations = 1,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxWidth(),
        )
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
                    .padding(horizontal = 20.dp)
                    .padding(top = 32.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WinnerPrizeCard(
                prizeAmount = prizeAmount,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(Res.string.winner_celebration_title, rank),
                    style = LocalAppTopography.current.xxlBold,
                    color = YralColors.Yellow200,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    text = stringResource(Res.string.winner_celebration_message, prizeAmount),
                    style = LocalAppTopography.current.mdRegular,
                    color = YralColors.Neutral300,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(38.dp))

                ViewLeaderboardButton(onClick = onViewLeaderboard)
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Suppress("MagicNumber")
@Composable
private fun WinnerPrizeCard(
    prizeAmount: String,
    modifier: Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(color = Color(0xFF1F1D17), shape = shape)
                .border(
                    width = 1.dp,
                    color = Color(0x6DFFF456),
                    shape = shape,
                ).padding(horizontal = 30.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(Res.string.winner_title),
            style =
                LocalAppTopography.current.mdMedium.copy(
                    fontSize = 20.sp,
                    lineHeight = 28.sp,
                ),
            color = Color(0xFFFFF9EB),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            YralMaskedVectorTextV2(
                text = prizeAmount,
                drawableRes = DesignRes.drawable.golden_gradient,
                textStyle =
                    TextStyle(
                        fontSize = 48.sp,
                        lineHeight = 67.2.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1.44).sp,
                        fontFamily = LocalAppTopography.current.mdBold.fontFamily,
                    ),
            )
            Image(
                painter = painterResource(Res.drawable.bitcoin),
                contentDescription = null,
                modifier = Modifier.size(54.dp),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
private fun buildWinnerBody(rank: Int) =
    buildAnnotatedString {
        append(stringResource(Res.string.winner_body_prefix))
        withStyle(
            style =
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                    color = YralColors.NeutralTextPrimary,
                ),
        ) {
            append(stringResource(Res.string.winner_rank_format, rank))
        }
        append("\n")
        append(stringResource(Res.string.winner_body_suffix))
    }

@Composable
private fun ViewLeaderboardButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(45.dp)
                .background(YralColors.Neutral50, shape)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val labelBrush = winnerPinkGradientBrush()
        val labelStyle =
            LocalAppTopography.current.mdBold.copy(
                brush = labelBrush,
            )
        Text(
            text = stringResource(Res.string.view_leaderboard),
            style = labelStyle,
        )
    }
}

@Suppress("MagicNumber")
private fun winnerPinkGradientBrush(): Brush =
    linearGradientBrush(
        angleDegrees = 189.5657f,
        size = Size(width = 240f, height = 45f),
        colorStops = winnerGradientStops(),
        angleConvention = GradientAngleConvention.CssDegrees,
        lengthMode = GradientLengthMode.Diagonal,
    ) ?: Brush.linearGradient(listOf(YralColors.Pink200, YralColors.Pink400))

@Suppress("MagicNumber")
private fun winnerGradientStops(): Array<Pair<Float, Color>> =
    arrayOf(
        0.0983f to Color(0xFFFF78C1),
        0.4479f to YralColors.Pink300,
        0.7848f to Color(0xFFAD005E),
    )

@Suppress("UnusedPrivateMember", "MagicNumber")
@Preview
@Composable
private fun TournamentWinnerScreenPreview() {
    CompositionLocalProvider(LocalAppTopography provides appTypoGraphy()) {
        TournamentWinnerScreen(
            prizeAmount = "₹10,000",
            rank = 1,
            onClose = {},
            onViewLeaderboard = { },
        )
    }
}
