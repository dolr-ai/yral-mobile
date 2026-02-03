package com.yral.shared.features.game.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yral.shared.libs.designsystem.component.YralGradientButton
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.features.game.generated.resources.Res
import yral_mobile.shared.features.game.generated.resources.hon_leaderboard_body
import yral_mobile.shared.features.game.generated.resources.hon_leaderboard_title
import yral_mobile.shared.features.game.generated.resources.hon_right_choice
import yral_mobile.shared.features.game.generated.resources.hon_right_choice_result
import yral_mobile.shared.features.game.generated.resources.hon_swipe_body
import yral_mobile.shared.features.game.generated.resources.hon_swipe_title
import yral_mobile.shared.features.game.generated.resources.hon_watch_video_body
import yral_mobile.shared.features.game.generated.resources.hon_watch_video_title
import yral_mobile.shared.features.game.generated.resources.hon_wrong_choice
import yral_mobile.shared.features.game.generated.resources.hon_wrong_choice_result
import yral_mobile.shared.features.game.generated.resources.how_to_play_question
import yral_mobile.shared.features.game.generated.resources.ic_hon_eyes
import yral_mobile.shared.features.game.generated.resources.ic_hon_hand
import yral_mobile.shared.features.game.generated.resources.ic_hon_leaderboard
import yral_mobile.shared.features.game.generated.resources.ic_yral_token
import yral_mobile.shared.features.game.generated.resources.keep_playing

@Composable
fun HotOrNotHowToPlayContent(
    onKeepPlayingClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(start = 16.dp, end = 16.dp, bottom = 28.dp),
    ) {
        Text(
            modifier = Modifier.padding(vertical = 12.dp),
            text = stringResource(Res.string.how_to_play_question),
            style = LocalAppTopography.current.xlBold,
        )
        Spacer(Modifier.height(12.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HowToPlayCard(
                iconRes = Res.drawable.ic_hon_eyes,
                title = stringResource(Res.string.hon_watch_video_title),
                body = AnnotatedString(stringResource(Res.string.hon_watch_video_body)),
            )
            SwipeHowToPlayCard()
            HowToPlayCard(
                iconRes = Res.drawable.ic_hon_leaderboard,
                title = stringResource(Res.string.hon_leaderboard_title),
                body = AnnotatedString(stringResource(Res.string.hon_leaderboard_body)),
            )
        }

        Spacer(Modifier.height(24.dp))

        YralGradientButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(Res.string.keep_playing),
            buttonHeight = 48.dp,
            onClick = onKeepPlayingClick,
        )
    }
}

@Composable
private fun HowToPlayCard(
    iconRes: DrawableResource,
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
                modifier = Modifier.size(ICON_SIZE),
            )
            Text(
                text = title,
                style = LocalAppTopography.current.lgBold,
                color = YralColors.Neutral50,
            )
        }
        Text(
            text = body,
            style = LocalAppTopography.current.baseRegular,
            color = YralColors.Neutral300,
            modifier = Modifier.padding(start = ICON_SIZE + ICON_TITLE_SPACING),
        )
    }
}

private val ICON_SIZE = 36.dp
private val ICON_TITLE_SPACING = 8.dp
private const val YRAL_TOKEN_ICON_ID = "yral_token"

@Composable
private fun SwipeHowToPlayCard() {
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
                painter = painterResource(Res.drawable.ic_hon_hand),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(ICON_SIZE),
            )
            Text(
                text = stringResource(Res.string.hon_swipe_title),
                style = LocalAppTopography.current.lgBold,
                color = YralColors.Neutral50,
            )
        }
        Text(
            text = buildSwipeBodyAnnotatedString(),
            style = LocalAppTopography.current.baseRegular,
            color = YralColors.Neutral300,
            inlineContent = yralTokenInlineContent(),
            modifier = Modifier.padding(start = ICON_SIZE + ICON_TITLE_SPACING),
        )
    }
}

@Composable
private fun buildSwipeBodyAnnotatedString(): AnnotatedString {
    val baseStyle = LocalAppTopography.current.baseRegular
    return buildAnnotatedString {
        withStyle(createBaseSpanStyle(baseStyle, YralColors.Neutral300)) {
            append(stringResource(Res.string.hon_swipe_body))
            append("\n")
            append(stringResource(Res.string.hon_right_choice))
        }
        withStyle(createBaseSpanStyle(baseStyle, YralColors.Green300)) {
            append(stringResource(Res.string.hon_right_choice_result))
        }
        append(" ")
        appendInlineContent(YRAL_TOKEN_ICON_ID, "[icon]")
        withStyle(createBaseSpanStyle(baseStyle, YralColors.Neutral300)) {
            append("\n")
            append(stringResource(Res.string.hon_wrong_choice))
        }
        withStyle(createBaseSpanStyle(baseStyle, YralColors.Red300)) {
            append(stringResource(Res.string.hon_wrong_choice_result))
        }
        append(" ")
        appendInlineContent(YRAL_TOKEN_ICON_ID, "[icon]")
    }
}

@Composable
private fun createBaseSpanStyle(
    baseStyle: TextStyle,
    color: Color,
) = SpanStyle(
    color = color,
    fontSize = baseStyle.fontSize,
    fontFamily = baseStyle.fontFamily,
    fontWeight = baseStyle.fontWeight,
)

@Composable
private fun yralTokenInlineContent() =
    mapOf(
        YRAL_TOKEN_ICON_ID to
            InlineTextContent(
                Placeholder(
                    width = 16.sp,
                    height = 16.sp,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                ),
            ) {
                Box(
                    modifier = Modifier.size(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(Res.drawable.ic_yral_token),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                }
            },
    )
