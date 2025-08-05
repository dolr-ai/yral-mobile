package com.yral.android.ui.screens.game

import android.graphics.Paint
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.design.YralColors.SmileyGameCardBackground
import com.yral.android.ui.screens.game.SmileyGameConstants.NUDGE_ANIMATION_DURATION

@Composable
fun GameStripBackground(
    modifier: Modifier,
    isShowingNudge: Boolean = false,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.SpaceBetween,
    content: @Composable () -> Unit,
) {
    val containerColor = SmileyGameCardBackground
    val cornerRadius = 49.dp
    val paddingValues = PaddingValues(horizontal = 16.dp, vertical = 14.dp)

    Box {
        if (isShowingNudge) {
            NudgeBorder(
                paddingValues = paddingValues,
                cornerRadius = cornerRadius,
                containerColor = containerColor,
            )
        }
        Row(modifier = modifier.padding(paddingValues)) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            color = containerColor,
                            shape = RoundedCornerShape(size = cornerRadius),
                        ).padding(horizontal = 12.dp, vertical = 9.dp),
                horizontalArrangement = horizontalArrangement,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                content()
            }
        }
    }
}

@Composable
private fun BoxScope.NudgeBorder(
    paddingValues: PaddingValues,
    cornerRadius: Dp,
    containerColor: Color,
) {
    val neonColor = Color.White.copy(alpha = 0.6f)
    val infiniteTransition = rememberInfiniteTransition(label = "infiniteBounce")
    val tweenSpec =
        tween<Float>(
            durationMillis = NUDGE_ANIMATION_DURATION.toInt(),
            easing = FastOutLinearInEasing,
        )
    val borderWidth by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(tweenSpec, RepeatMode.Reverse),
        label = "border",
    )
    Box(
        modifier =
            Modifier
                .matchParentSize()
                .padding(paddingValues)
                .background(
                    color = containerColor,
                    shape = RoundedCornerShape(size = cornerRadius),
                ).neonBorder(
                    glowingColor = neonColor,
                    containerColor = YralColors.ScrimColorLight,
                    glowingRadius = borderWidth.dp,
                    cornerRadius = cornerRadius,
                ),
    )
}

private fun Modifier.neonBorder(
    glowingColor: Color,
    containerColor: Color,
    glowingRadius: Dp,
    cornerRadius: Dp,
    xShifting: Dp = 0.dp,
    yShifting: Dp = 0.dp,
) = this.drawBehind {
    val canvasSize = size
    drawContext.canvas.nativeCanvas.apply {
        drawRoundRect(
            0f, // Left
            0f, // Top
            canvasSize.width, // Right
            canvasSize.height, // Bottom
            cornerRadius.toPx(), // Radius X
            cornerRadius.toPx(), // Radius Y
            Paint().apply {
                color = containerColor.toArgb()
                isAntiAlias = true
                setShadowLayer(
                    glowingRadius.toPx(),
                    xShifting.toPx(),
                    yShifting.toPx(),
                    glowingColor.toArgb(),
                )
            },
        )
    }
}
