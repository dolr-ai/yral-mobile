@file:Suppress("MagicNumber")

package com.yral.shared.app.ui.screens.dailystreak

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yral.shared.libs.designsystem.component.YralGradientButton
import com.yral.shared.libs.designsystem.component.YralMaskedVectorTextV2
import com.yral.shared.libs.designsystem.theme.GradientAngleConvention
import com.yral.shared.libs.designsystem.theme.GradientLengthMode
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.libs.designsystem.theme.angledGradientBackground
import com.yral.shared.libs.designsystem.theme.kumbhSansFontFamily
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import yral_mobile.shared.app.generated.resources.Res
import yral_mobile.shared.app.generated.resources.daily_streak_action
import yral_mobile.shared.app.generated.resources.daily_streak_label
import yral_mobile.shared.app.generated.resources.daily_streak_started_title
import yral_mobile.shared.app.generated.resources.daily_streak_subtitle
import yral_mobile.shared.libs.designsystem.generated.resources.fire
import yral_mobile.shared.libs.designsystem.generated.resources.golden_gradient
import yral_mobile.shared.libs.designsystem.generated.resources.ic_lightning_bolt_gold
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

private val ScreenGradientTop = Color(0xFF342735)
private val ScreenGradientMid = Color(0xFF1B1623)
private val ScreenGradientBottom = Color(0xFF050507)
private val BackgroundRayBright = Color(0xFFF2B1AC)
private val BackgroundRaySoft = Color(0xFFA47987)
private val BackgroundSideShadow = Color(0xFF241A31)
private val BadgeFillStart = Color(0xFFFFE07A)
private val BadgeFillEnd = Color(0xFFFF8A2A)
private val BadgeInnerFill = Color(0xFFD9308B)
private val ContentHorizontalPadding = 18.dp
private val TopContentSpacer = 96.dp
private val HeroCountSpacing = 18.dp
private val CountCopySpacing = 72.dp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DailyStreakCelebrationScreen(
    streakCount: Long,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)

    Box(
        modifier =
            Modifier
                .fillMaxSize(),
    ) {
        BackgroundImage()
        CloseButton(onDismiss = onDismiss)

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = ContentHorizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(TopContentSpacer))
            StreakHero()
            Spacer(modifier = Modifier.height(HeroCountSpacing))
            StreakCount(streakCount = streakCount)
            Spacer(modifier = Modifier.height(CountCopySpacing))
            BottomContent(onDismiss = onDismiss)
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun BackgroundImage() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    brush =
                        Brush.verticalGradient(
                            colors = listOf(ScreenGradientTop, ScreenGradientMid, ScreenGradientBottom),
                        ),
                ),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .angledGradientBackground(
                        colorStops =
                            arrayOf(
                                0.00f to BackgroundRayBright.copy(alpha = 0.90f),
                                0.16f to BackgroundRaySoft.copy(alpha = 0.72f),
                                0.40f to Color.Transparent,
                            ),
                        degrees = 108f,
                        angleConvention = GradientAngleConvention.CssDegrees,
                        lengthMode = GradientLengthMode.Diagonal,
                    ),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .angledGradientBackground(
                        colorStops =
                            arrayOf(
                                0.00f to Color.Transparent,
                                0.08f to BackgroundRayBright.copy(alpha = 0.92f),
                                0.22f to BackgroundRaySoft.copy(alpha = 0.70f),
                                0.44f to Color.Transparent,
                            ),
                        degrees = 90f,
                        angleConvention = GradientAngleConvention.CssDegrees,
                        lengthMode = GradientLengthMode.Diagonal,
                    ),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .angledGradientBackground(
                        colorStops =
                            arrayOf(
                                0.00f to BackgroundRayBright.copy(alpha = 0.88f),
                                0.14f to BackgroundRaySoft.copy(alpha = 0.68f),
                                0.38f to Color.Transparent,
                            ),
                        degrees = 72f,
                        angleConvention = GradientAngleConvention.CssDegrees,
                        lengthMode = GradientLengthMode.Diagonal,
                    ),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        brush =
                            Brush.horizontalGradient(
                                colorStops =
                                    arrayOf(
                                        0.00f to BackgroundSideShadow.copy(alpha = 0.62f),
                                        0.20f to Color.Transparent,
                                        0.80f to Color.Transparent,
                                        1.00f to BackgroundSideShadow.copy(alpha = 0.58f),
                                    ),
                            ),
                    ),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        brush =
                            Brush.verticalGradient(
                                colorStops =
                                    arrayOf(
                                        0.00f to Color.Transparent,
                                        0.62f to Color.Transparent,
                                        0.84f to ScreenGradientBottom.copy(alpha = 0.92f),
                                        1.00f to ScreenGradientBottom,
                                    ),
                            ),
                    ),
        )
    }
}

@Composable
private fun CloseButton(onDismiss: () -> Unit) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 14.dp, end = 18.dp),
        contentAlignment = Alignment.TopEnd,
    ) {
        Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = null,
            tint = YralColors.NeutralTextPrimary.copy(alpha = 0.92f),
            modifier =
                Modifier
                    .size(24.dp)
                    .clickable(onClick = onDismiss),
        )
    }
}

@Composable
private fun StreakHero() {
    Box(
        modifier = Modifier.size(170.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(DesignRes.drawable.fire),
            contentDescription = null,
            modifier =
                Modifier
                    .size(148.dp)
                    .shadow(22.dp, CircleShape, ambientColor = YralColors.Pink200, spotColor = YralColors.Pink200),
            contentScale = ContentScale.Fit,
        )

        Badge(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 30.dp, bottom = 10.dp),
        )
    }
}

@Composable
private fun Badge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(52.dp),
        shape = CircleShape,
        color = Color.Transparent,
        shadowElevation = 14.dp,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(colors = listOf(BadgeFillStart, BadgeFillEnd)),
                    ).padding(4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(BadgeInnerFill),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(DesignRes.drawable.ic_lightning_bolt_gold),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun StreakCount(streakCount: Long) {
    val countStyle =
        TextStyle(
            fontFamily = kumbhSansFontFamily(),
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            lineHeight = 30.sp,
            textAlign = TextAlign.Center,
            shadow = Shadow(color = YralColors.YellowGlowShadow, blurRadius = 18f),
        )

    val labelStyle =
        TextStyle(
            fontFamily = kumbhSansFontFamily(),
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 28.sp,
            textAlign = TextAlign.Center,
            shadow = Shadow(color = YralColors.YellowGlowShadow, blurRadius = 18f),
        )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        YralMaskedVectorTextV2(
            text = streakCount.toString(),
            drawableRes = DesignRes.drawable.golden_gradient,
            textStyle = countStyle,
        )
        Spacer(modifier = Modifier.height(2.dp))
        YralMaskedVectorTextV2(
            text = stringResource(Res.string.daily_streak_label),
            drawableRes = DesignRes.drawable.golden_gradient,
            textStyle = labelStyle,
        )
    }
}

@Composable
private fun BottomContent(onDismiss: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.daily_streak_started_title),
            modifier = Modifier.padding(horizontal = 12.dp),
            style = LocalAppTopography.current.xlBold,
            color = YralColors.NeutralTextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(Res.string.daily_streak_subtitle),
            modifier = Modifier.padding(horizontal = 12.dp),
            style = LocalAppTopography.current.mdRegular,
            color = YralColors.NeutralTextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        YralGradientButton(
            text = stringResource(Res.string.daily_streak_action),
            buttonHeight = 42.dp,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp)),
            onClick = onDismiss,
        )
    }
}

@Suppress("UnusedPrivateMember")
@Preview
@Composable
private fun DailyStreakCelebrationScreenPreview() {
    DailyStreakCelebrationScreen(
        streakCount = 1,
        onDismiss = {},
    )
}
