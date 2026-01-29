package com.yral.shared.features.subscriptions.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import yral_mobile.shared.libs.designsystem.generated.resources.Res
import yral_mobile.shared.libs.designsystem.generated.resources.ic_lightning_bolt_gold

@Composable
fun BoltIcon(
    boltIcon: DrawableResource = Res.drawable.ic_lightning_bolt_gold,
    gradientColors: List<Color> =
        listOf(
            YralColors.YellowGlowShadow.copy(alpha = 0.3f),
            YralColors.YellowGlowShadow.copy(alpha = 0.1f),
            Color.Transparent,
        ),
    gradientRadius: Float = 200f,
    modifier: Modifier = Modifier.size(width = 200.dp, height = 180.dp),
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .background(brush = Brush.radialGradient(colors = gradientColors, radius = gradientRadius)),
    ) {
        Image(
            painter = painterResource(boltIcon),
            contentDescription = "Subscription Logo",
            modifier = Modifier.size(width = 74.dp, height = 120.dp),
        )
    }
}
