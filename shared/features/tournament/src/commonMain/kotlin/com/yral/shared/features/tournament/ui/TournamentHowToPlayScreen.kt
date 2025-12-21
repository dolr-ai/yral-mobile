package com.yral.shared.features.tournament.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yral.shared.libs.designsystem.component.YralGradientButton
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.libs.designsystem.theme.appTypoGraphy
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import yral_mobile.shared.features.tournament.generated.resources.Res
import yral_mobile.shared.features.tournament.generated.resources.body_live_leaderboard
import yral_mobile.shared.features.tournament.generated.resources.body_pick_emoji
import yral_mobile.shared.features.tournament.generated.resources.body_pick_emoji_right_choice
import yral_mobile.shared.features.tournament.generated.resources.body_pick_emoji_right_choice_result
import yral_mobile.shared.features.tournament.generated.resources.body_pick_emoji_wrong_choice
import yral_mobile.shared.features.tournament.generated.resources.body_pick_emoji_wrong_choice_result
import yral_mobile.shared.features.tournament.generated.resources.body_watch_video
import yral_mobile.shared.features.tournament.generated.resources.each_tournament_line_one
import yral_mobile.shared.features.tournament.generated.resources.each_tournament_line_two
import yral_mobile.shared.features.tournament.generated.resources.how_to_play
import yral_mobile.shared.features.tournament.generated.resources.start_playing
import yral_mobile.shared.features.tournament.generated.resources.title_live_leaderboard
import yral_mobile.shared.features.tournament.generated.resources.title_pick_emoji
import yral_mobile.shared.features.tournament.generated.resources.title_watch_video
import yral_mobile.shared.features.tournament.generated.resources.tournament_diamond
import yral_mobile.shared.features.tournament.generated.resources.tournament_eyes
import yral_mobile.shared.features.tournament.generated.resources.tournament_hand
import yral_mobile.shared.features.tournament.generated.resources.tournament_leaderboard
import yral_mobile.shared.features.tournament.generated.resources.you_are_starting_with

@Suppress("LongMethod", "MagicNumber")
@Composable
fun TournamentHowToPlayScreen(
    title: String,
    onStartPlaying: () -> Unit,
    modifier: Modifier = Modifier,
    startingDiamonds: Int = 100,
) {
    Box(
        modifier =
            modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    colors =
                        listOf(
                            Color(0xB3000000),
                            Color(0xCC000000),
                            Color(0xFF000000),
                        ),
                ),
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 110.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = title,
                    style = LocalAppTopography.current.xxlBold,
                    color = YralColors.Yellow200,
                    textAlign = TextAlign.Center,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.you_are_starting_with, startingDiamonds),
                        style =
                            LocalAppTopography.current.lgMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                        color = YralColors.NeutralTextPrimary,
                    )
                    Image(
                        painter = painterResource(Res.drawable.tournament_diamond),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(Res.string.how_to_play),
                style = LocalAppTopography.current.mdSemiBold,
                color = YralColors.Neutral50,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HowToPlayCard(
                    iconRes = Res.drawable.tournament_eyes,
                    iconWidth = 45.dp,
                    iconHeight = 34.dp,
                    title = stringResource(Res.string.title_watch_video),
                    body = AnnotatedString(stringResource(Res.string.body_watch_video)),
                )
                HowToPlayCard(
                    iconRes = Res.drawable.tournament_hand,
                    iconWidth = 36.dp,
                    iconHeight = 44.dp,
                    title = stringResource(Res.string.title_pick_emoji),
                    body = buildPickEmojiBody(),
                )
                HowToPlayCard(
                    iconRes = Res.drawable.tournament_leaderboard,
                    iconWidth = 41.dp,
                    iconHeight = 42.dp,
                    title = stringResource(Res.string.title_live_leaderboard),
                    body = AnnotatedString(stringResource(Res.string.body_live_leaderboard)),
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text =
                    listOf(
                        stringResource(Res.string.each_tournament_line_one),
                        stringResource(Res.string.each_tournament_line_two),
                    ).joinToString("\n"),
                style = LocalAppTopography.current.mdSemiBold,
                color = YralColors.NeutralTextPrimary,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(28.dp))

            YralGradientButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(Res.string.start_playing),
                buttonHeight = 48.dp,
                onClick = onStartPlaying,
            )
        }
    }
}

@Composable
private fun HowToPlayCard(
    iconRes: DrawableResource,
    iconWidth: Dp,
    iconHeight: Dp,
    title: String,
    body: AnnotatedString,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = YralColors.Neutral700,
                    shape = RoundedCornerShape(8.dp),
                ).background(
                    color = YralColors.NeutralBackgroundCardBackground,
                    shape = RoundedCornerShape(8.dp),
                ).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.width(iconWidth).height(iconHeight),
            )
            Text(
                text = title,
                style =
                    LocalAppTopography.current.lgBold.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                color = YralColors.Neutral50,
            )
        }
        Text(
            text = body,
            style = LocalAppTopography.current.baseRegular,
            color = YralColors.Neutral300,
        )
    }
}

@Composable
private fun buildPickEmojiBody(): AnnotatedString {
    val baseStyle = LocalAppTopography.current.baseRegular
    return buildAnnotatedString {
        withStyle(
            SpanStyle(
                color = YralColors.Neutral300,
                fontSize = baseStyle.fontSize,
                fontFamily = baseStyle.fontFamily,
                fontWeight = baseStyle.fontWeight,
            ),
        ) {
            append(stringResource(Res.string.body_pick_emoji))
            append("\n")
            append(stringResource(Res.string.body_pick_emoji_right_choice))
        }
        withStyle(
            SpanStyle(
                color = YralColors.Green300,
                fontSize = baseStyle.fontSize,
                fontFamily = baseStyle.fontFamily,
                fontWeight = baseStyle.fontWeight,
            ),
        ) {
            append(stringResource(Res.string.body_pick_emoji_right_choice_result))
        }
        withStyle(
            SpanStyle(
                color = YralColors.Neutral300,
                fontSize = baseStyle.fontSize,
                fontFamily = baseStyle.fontFamily,
                fontWeight = baseStyle.fontWeight,
            ),
        ) {
            append("\n")
            append(stringResource(Res.string.body_pick_emoji_wrong_choice))
        }
        withStyle(
            SpanStyle(
                color = YralColors.Red300,
                fontSize = baseStyle.fontSize,
                fontFamily = baseStyle.fontFamily,
                fontWeight = baseStyle.fontWeight,
            ),
        ) {
            append(stringResource(Res.string.body_pick_emoji_wrong_choice_result))
        }
    }
}

@Suppress("UnusedPrivateMember")
@Preview
@Composable
private fun TournamentHowToPlayScreenPreview() {
    CompositionLocalProvider(LocalAppTopography provides appTypoGraphy()) {
        TournamentHowToPlayScreen(title = "THE SMILY SHOWDOWN", onStartPlaying = {})
    }
}
