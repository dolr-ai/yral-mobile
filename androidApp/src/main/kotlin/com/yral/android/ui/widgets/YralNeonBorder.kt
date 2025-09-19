package com.yral.android.ui.widgets

import android.graphics.Paint
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yral.shared.libs.designsystem.theme.YralColors

@Composable
fun BoxScope.YralNeonBorder(
    paddingValues: PaddingValues,
    cornerRadius: Dp,
    containerColor: Color,
    animationDuration: Long,
    neonColor: Color = Color.White.copy(alpha = 0.6f),
    borderWidth: Float = 8f,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "infiniteBounce")
    val tweenSpec =
        tween<Float>(
            durationMillis = animationDuration.toInt(),
            easing = FastOutLinearInEasing,
        )
    val borderWidth by infiniteTransition.animateFloat(
        initialValue = borderWidth,
        targetValue = 0f,
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
