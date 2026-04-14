@file:Suppress("MagicNumber")

package com.yral.shared.app.ui.screens.dailystreak

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.yral.shared.libs.designsystem.component.YralGradientButton
import com.yral.shared.libs.designsystem.component.getSVGImageModel
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.libs.designsystem.theme.kumbhSansFontFamily
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import yral_mobile.shared.app.generated.resources.Res
import yral_mobile.shared.app.generated.resources.daily_streak_action
import yral_mobile.shared.app.generated.resources.daily_streak_badge
import yral_mobile.shared.app.generated.resources.daily_streak_close_image
import yral_mobile.shared.app.generated.resources.daily_streak_label
import yral_mobile.shared.app.generated.resources.daily_streak_noise_texture
import yral_mobile.shared.app.generated.resources.daily_streak_started_title
import yral_mobile.shared.app.generated.resources.daily_streak_subtitle
import kotlin.math.min

private const val REFERENCE_FRAME_WIDTH = 390f
private const val REFERENCE_FRAME_HEIGHT = 852f
private const val CLOSE_TOP = 67f
private const val CLOSE_END = 20f
private const val HERO_TOP = 148f
private const val HERO_WIDTH = 126.87955f
private const val HERO_HEIGHT = 142f
private const val FLAME_WIDTH = 119.75895f
private const val BADGE_WIDTH = 58f
private const val BADGE_HEIGHT = 59f
private const val COUNT_TOP = 317f
private const val COUNT_WIDTH = 163f
private const val CONTENT_TOP = 467f
private const val CONTENT_WIDTH = 350f
private const val BUTTON_HEIGHT = 42f
private const val FLAME_ASSET_PATH = "drawable/daily_streak_flame.svg"

@Composable
fun DailyStreakCelebrationScreen(
    streakCount: Long,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)

    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxSize()
                .background(YralColors.Neutral950),
    ) {
        val frameScale = min(maxWidth.value / REFERENCE_FRAME_WIDTH, maxHeight.value / REFERENCE_FRAME_HEIGHT)

        Box(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .width(scaledDp(REFERENCE_FRAME_WIDTH, frameScale))
                    .height(scaledDp(REFERENCE_FRAME_HEIGHT, frameScale)),
        ) {
            BackgroundImage()
            CloseButton(
                frameScale = frameScale,
                onDismiss = onDismiss,
            )
            StreakHero(frameScale = frameScale)
            StreakCount(
                frameScale = frameScale,
                streakCount = streakCount,
            )
            BottomContent(frameScale = frameScale, onDismiss = onDismiss)
        }
    }
}

@Composable
private fun BackgroundImage() {
    Image(
        painter = painterResource(Res.drawable.daily_streak_noise_texture),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun BoxScope.CloseButton(
    frameScale: Float,
    onDismiss: () -> Unit,
) {
    Image(
        painter = painterResource(Res.drawable.daily_streak_close_image),
        contentDescription = null,
        modifier =
            Modifier
                .align(Alignment.TopEnd)
                .absoluteOffset(
                    x = -scaledDp(CLOSE_END, frameScale),
                    y = scaledDp(CLOSE_TOP, frameScale),
                ).size(scaledDp(24f, frameScale))
                .clickable(onClick = onDismiss),
    )
}

@Composable
private fun BoxScope.StreakHero(frameScale: Float) {
    Box(
        modifier =
            Modifier
                .align(Alignment.TopCenter)
                .absoluteOffset(y = scaledDp(HERO_TOP, frameScale))
                .width(scaledDp(HERO_WIDTH, frameScale))
                .height(scaledDp(HERO_HEIGHT, frameScale)),
    ) {
        AsyncImage(
            model = getSVGImageModel(Res.getUri(FLAME_ASSET_PATH)),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .width(scaledDp(FLAME_WIDTH, frameScale))
                    .height(scaledDp(HERO_HEIGHT, frameScale)),
        )

        Image(
            painter = painterResource(Res.drawable.daily_streak_badge),
            contentDescription = null,
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .width(scaledDp(BADGE_WIDTH, frameScale))
                    .height(scaledDp(BADGE_HEIGHT, frameScale)),
        )
    }
}

@Composable
private fun BoxScope.StreakCount(
    frameScale: Float,
    streakCount: Long,
) {
    Text(
        text = "$streakCount\n${stringResource(Res.string.daily_streak_label)}",
        modifier =
            Modifier
                .align(Alignment.TopCenter)
                .absoluteOffset(y = scaledDp(COUNT_TOP, frameScale))
                .width(scaledDp(COUNT_WIDTH, frameScale)),
        style = streakCountTextStyle(frameScale),
        color = YralColors.Yellow200,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun BoxScope.BottomContent(
    frameScale: Float,
    onDismiss: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .align(Alignment.TopCenter)
                .absoluteOffset(y = scaledDp(CONTENT_TOP, frameScale))
                .width(scaledDp(CONTENT_WIDTH, frameScale)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(scaledDp(20f, frameScale)),
    ) {
        Text(
            text = stringResource(Res.string.daily_streak_started_title),
            modifier = Modifier.width(scaledDp(CONTENT_WIDTH, frameScale)),
            style =
                LocalAppTopography.current.xlBold.copy(
                    textAlign = TextAlign.Center,
                ),
            color = YralColors.NeutralTextPrimary,
        )
        Text(
            text = stringResource(Res.string.daily_streak_subtitle),
            modifier = Modifier.width(scaledDp(CONTENT_WIDTH, frameScale)),
            style =
                LocalAppTopography.current.mdRegular.copy(
                    textAlign = TextAlign.Center,
                ),
            color = YralColors.Neutral300,
        )
        YralGradientButton(
            text = stringResource(Res.string.daily_streak_action),
            textStyle =
                LocalAppTopography.current.mdBold.copy(
                    textAlign = TextAlign.Center,
                ),
            buttonHeight = scaledDp(BUTTON_HEIGHT, frameScale),
            modifier =
                Modifier
                    .width(scaledDp(CONTENT_WIDTH, frameScale))
                    .clip(
                        shape = RoundedCornerShape(scaledDp(8f, frameScale)),
                    ),
            onClick = onDismiss,
        )
    }
}

@Composable
private fun streakCountTextStyle(frameScale: Float): TextStyle =
    TextStyle(
        fontFamily = kumbhSansFontFamily(),
        fontWeight = FontWeight.Bold,
        fontSize = scaledSp(32f, frameScale),
        lineHeight = scaledSp(44.8f, frameScale),
        letterSpacing = scaledSp(-0.64f, frameScale),
        textAlign = TextAlign.Center,
    )

private fun scaledDp(
    value: Float,
    scale: Float,
): Dp = value.dp * scale

private fun scaledSp(
    value: Float,
    scale: Float,
) = value.sp * scale

@Suppress("UnusedPrivateMember")
@Preview
@Composable
private fun DailyStreakCelebrationScreenPreview() {
    DailyStreakCelebrationScreen(
        streakCount = 1,
        onDismiss = {},
    )
}
