package com.yral.shared.libs.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yral.shared.libs.designsystem.component.YralShimmerConstants.ANIMATION_DURATION
import com.yral.shared.libs.designsystem.component.YralShimmerConstants.WIDTH_FACTOR
import com.yral.shared.libs.designsystem.component.YralShimmerConstants.shimmerColors

@Suppress("MagicNumber")
private object YralShimmerConstants {
    val shimmerColors =
        listOf(
            Color(0xFF393939),
            Color(0xFF1E1E1E),
        )
    const val ANIMATION_DURATION = 600
    const val WIDTH_FACTOR = 1.5f
}

@Composable
fun <T> YralShimmerView(
    data: T?,
    placeholderData: T,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(T) -> Unit,
) {
    Box(
        modifier = if (data == null) modifier.shimmer(cornerRadius = 8.dp) else modifier,
        content = { content(data ?: placeholderData) },
    )
}

@Composable
fun Modifier.shimmer(cornerRadius: Dp = 0.dp) =
    composed {
        var sizePx by remember { mutableStateOf(Size.Zero) }
        val animatedOffset = remember { Animatable(0f) }
        LaunchedEffect(sizePx) {
            if (sizePx.width > 0f) {
                animatedOffset.snapTo(-sizePx.width)
                animatedOffset.animateTo(
                    targetValue = sizePx.width * 2,
                    animationSpec =
                        infiniteRepeatable(
                            animation = tween(ANIMATION_DURATION, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse,
                        ),
                )
            }
        }
        this.then(
            Modifier.drawWithCache {
                sizePx = size
                val brush =
                    Brush.linearGradient(
                        colors = shimmerColors,
                        start = Offset(animatedOffset.value, 0f),
                        end = Offset(animatedOffset.value + size.width / WIDTH_FACTOR, size.height),
                    )
                onDrawWithContent {
                    drawRoundRect(
                        brush = brush,
                        cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
                        size = size,
                    )
                }
            },
        )
    }
