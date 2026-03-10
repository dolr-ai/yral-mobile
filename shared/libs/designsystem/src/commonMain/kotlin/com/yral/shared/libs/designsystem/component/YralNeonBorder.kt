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
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val NEON_GLOW_LAYERS = 4
private const val NEON_GLOW_ALPHA = 0.15f

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
        // Do NOT delegate with `by` — keep as State<Float> so it's not read during composition.
        val borderWidthState =
            infiniteTransition.animateFloat(
                initialValue = initialBorderWidth,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(tweenSpec, RepeatMode.Reverse),
                label = "border",
            )
        val density = LocalDensity.current
        val cornerRadiusPx = with(density) { cornerRadius.toPx() }
        this
            .padding(paddingValues)
            .drawBehind {
                // Read animated state ONLY in draw phase → no recomposition, only redraw.
                val spreadPx = with(density) { borderWidthState.value.dp.toPx() }
                if (spreadPx > 0f) {
                    for (i in NEON_GLOW_LAYERS downTo 0) {
                        val fraction = i.toFloat() / NEON_GLOW_LAYERS
                        val layerSpread = spreadPx * (1f + fraction)
                        drawRoundRect(
                            color = neonColor.copy(alpha = NEON_GLOW_ALPHA * (1f - fraction)),
                            cornerRadius = CornerRadius(cornerRadiusPx + layerSpread),
                            topLeft = Offset(-layerSpread, -layerSpread),
                            size =
                                Size(
                                    size.width + layerSpread * 2,
                                    size.height + layerSpread * 2,
                                ),
                        )
                    }
                }
            }.background(
                color = containerColor,
                shape = RoundedCornerShape(cornerRadius),
            )
    }
