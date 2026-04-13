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
import androidx.compose.ui.Modifier
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
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
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

private val ScreenGradientTop = Color(0xFF2A1022)
private val ScreenGradientMid = Color(0xFF15111E)
private val ScreenGradientBottom = Color(0xFF050507)
private val GlowColor = Color(0xFFF8A8B8)
private val BadgeFillStart = Color(0xFFFFE07A)
private val BadgeFillEnd = Color(0xFFFF8A2A)
private val BottomSection = Color(0xFF020202)

@Composable
fun DailyStreakCelebrationScreen(
    streakCount: Long,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)

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
        BackgroundGlow()
        CloseButton(onDismiss = onDismiss)

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(88.dp))
            StreakHero()
            Spacer(modifier = Modifier.height(24.dp))
            StreakCount(streakCount = streakCount)
            Spacer(modifier = Modifier.weight(1f))
            BottomContent(onDismiss = onDismiss)
        }
    }
}

@Composable
private fun BackgroundGlow() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(320.dp)
                .background(
                    brush =
                        Brush.radialGradient(
                            colors = listOf(GlowColor.copy(alpha = 0.85f), Color.Transparent),
                            radius = 900f,
                        ),
                ),
    )
}

@Composable
private fun CloseButton(onDismiss: () -> Unit) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 16.dp, end = 20.dp),
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
        modifier = Modifier.size(180.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(DesignRes.drawable.fire),
            contentDescription = null,
            modifier =
                Modifier
                    .size(156.dp)
                    .shadow(36.dp, CircleShape, ambientColor = YralColors.Pink200, spotColor = YralColors.Pink200),
            contentScale = ContentScale.Fit,
        )

        Badge(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 34.dp, bottom = 12.dp),
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
                        .background(Color(0xFFD9308B)),
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
        modifier =
            Modifier
                .fillMaxWidth()
                .background(BottomSection)
                .padding(horizontal = 20.dp, vertical = 28.dp)
                .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.daily_streak_started_title),
            style = LocalAppTopography.current.xlBold,
            color = YralColors.NeutralTextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(Res.string.daily_streak_subtitle),
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
