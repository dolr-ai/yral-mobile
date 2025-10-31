package com.yral.shared.libs.designsystem.component

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

fun Modifier.neonBorder(
    paddingValues: PaddingValues,
    cornerRadius: Dp,
    containerColor: Color,
    animationDuration: Long,
    neonColor: Color = Color.White.copy(alpha = 0.6f),
    initialBorderWidth: Float = 8f,
): Modifier =
    composed {
        val infiniteTransition = rememberInfiniteTransition(label = "infiniteBounce")
        val tweenSpec =
            tween<Float>(
                durationMillis = animationDuration.toInt(),
                easing = FastOutLinearInEasing,
            )
        val borderWidth by infiniteTransition.animateFloat(
            initialValue = initialBorderWidth,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(tweenSpec, RepeatMode.Reverse),
            label = "border",
        )
        this
            .padding(paddingValues)
            .background(
                color = containerColor,
                shape = RoundedCornerShape(cornerRadius),
            ).dropShadow(
                shape = RoundedCornerShape(cornerRadius),
                shadow =
                    Shadow(
                        radius = 10.dp,
                        spread = borderWidth.dp,
                        brush = Brush.sweepGradient(listOf(neonColor, neonColor)),
                        offset = DpOffset(0.dp, 0.dp),
                        alpha = 0.6f,
                        blendMode = BlendMode.SrcOver,
                    ),
            )
    }
