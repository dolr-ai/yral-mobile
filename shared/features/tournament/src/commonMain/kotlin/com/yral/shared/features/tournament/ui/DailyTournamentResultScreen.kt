package com.yral.shared.features.tournament.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.yral.shared.libs.designsystem.component.lottie.LottieRes
import com.yral.shared.libs.designsystem.component.lottie.YralLottieAnimation
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.libs.designsystem.theme.appTypoGraphy
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import yral_mobile.shared.features.tournament.generated.resources.Res
import yral_mobile.shared.features.tournament.generated.resources.confettie
import yral_mobile.shared.features.tournament.generated.resources.daily_result_collected_today
import yral_mobile.shared.features.tournament.generated.resources.daily_result_current_rank
import yral_mobile.shared.features.tournament.generated.resources.daily_result_diamonds_collected
import yral_mobile.shared.features.tournament.generated.resources.daily_result_subtitle
import yral_mobile.shared.features.tournament.generated.resources.daily_result_view_leaderboard
import yral_mobile.shared.features.tournament.generated.resources.tournament_diamond
import yral_mobile.shared.features.tournament.generated.resources.tournament_noise_texture
import yral_mobile.shared.libs.designsystem.generated.resources.cross
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@Suppress("MagicNumber")
private val GoldColor = Color(0xFFFFC33A)

@Suppress("MagicNumber")
private val BlueGlowColor = Color(0x996CBBC6)

@Suppress("LongMethod", "MagicNumber")
@Composable
fun DailyTournamentResultScreen(
    diamonds: Int,
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
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(Res.drawable.tournament_diamond),
                contentDescription = null,
                modifier =
                    Modifier
                        .size(120.dp)
                        .shadow(
                            elevation = 24.dp,
                            shape = CircleShape,
                            ambientColor = BlueGlowColor,
                            spotColor = BlueGlowColor,
                        ),
                contentScale = ContentScale.Fit,
            )
            Spacer(modifier = Modifier.height(24.dp))
            DiamondsCollectedText(diamonds = diamonds)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(Res.string.daily_result_current_rank, rank),
                style = LocalAppTopography.current.xlBold,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                modifier = Modifier.padding(horizontal = 24.dp),
                text = stringResource(Res.string.daily_result_subtitle),
                style = LocalAppTopography.current.mdRegular,
                color = YralColors.Neutral500,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(38.dp))
            DailyViewLeaderboardButton(onClick = onViewLeaderboard)
        }
    }
}

@Composable
private fun DiamondsCollectedText(diamonds: Int) {
    val annotatedString =
        buildAnnotatedString {
            withStyle(SpanStyle(color = GoldColor)) {
                append(stringResource(Res.string.daily_result_diamonds_collected, diamonds))
            }
            withStyle(SpanStyle(color = Color.White)) {
                append(stringResource(Res.string.daily_result_collected_today))
            }
        }
    Text(
        text = annotatedString,
        style = LocalAppTopography.current.xxlBold,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun DailyViewLeaderboardButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(shape)
                .background(YralColors.Neutral800)
                .border(1.dp, YralColors.Neutral700, shape)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(Res.string.daily_result_view_leaderboard),
            style = LocalAppTopography.current.baseSemiBold,
            color = YralColors.NeutralIconsActive,
        )
    }
}

@Suppress("UnusedPrivateMember", "MagicNumber")
@Preview
@Composable
private fun DailyTournamentResultScreenPreview() {
    CompositionLocalProvider(LocalAppTopography provides appTypoGraphy()) {
        DailyTournamentResultScreen(
            diamonds = 150,
            rank = 3,
            onClose = {},
            onViewLeaderboard = {},
        )
    }
}
