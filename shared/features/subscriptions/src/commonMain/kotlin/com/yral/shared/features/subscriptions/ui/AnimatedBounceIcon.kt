package com.yral.shared.features.subscriptions.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun AnimatedBounceIcon(
    modifier: Modifier = Modifier,
    imageRes: DrawableResource,
    size: Dp = AnimatedBounceIconConstants.IMAGE_SIZE,
    animationCompleted: () -> Unit = {},
) {
    var animationStep by remember { mutableIntStateOf(0) }

    val scaleValues = AnimatedBounceIconConstants.SCALE_VALUES
    val opacityValues = AnimatedBounceIconConstants.OPACITY_VALUES

    val animatedScale by animateFloatAsState(
        targetValue =
            scaleValues.getOrElse(animationStep) {
                AnimatedBounceIconConstants.DEFAULT_SCALE
            },
        animationSpec =
            tween(
                durationMillis = AnimatedBounceIconConstants.KEYFRAME_DURATION,
                easing = FastOutSlowInEasing,
            ),
        label = "scale",
    )

    val animatedOpacity by animateFloatAsState(
        targetValue =
            opacityValues.getOrElse(animationStep) {
                AnimatedBounceIconConstants.DEFAULT_OPACITY
            },
        animationSpec =
            tween(
                durationMillis = AnimatedBounceIconConstants.KEYFRAME_DURATION,
                easing = FastOutSlowInEasing,
            ),
        label = "opacity",
    )

    LaunchedEffect(animationStep) {
        if (animationStep < scaleValues.size - 1) {
            delay(AnimatedBounceIconConstants.KEYFRAME_DELAY)
            animationStep++
        } else if (animationStep == scaleValues.size - 1) {
            animationCompleted()
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = "Animated bounce icon",
            modifier =
                Modifier
                    .size(size)
                    .graphicsLayer(
                        scaleX = animatedScale,
                        scaleY = animatedScale,
                        alpha = animatedOpacity,
                    ),
        )
    }
}

@Suppress("MagicNumber")
private object AnimatedBounceIconConstants {
    const val KEYFRAME_DURATION = 180
    const val KEYFRAME_DELAY = 200L
    const val DEFAULT_SCALE = 1f
    const val DEFAULT_OPACITY = 1f

    val IMAGE_SIZE = 120.dp

    val SCALE_VALUES = listOf(0f, 1.5f, 0.9f, 1.08f, 0.95f, 1.05f, 1f)
    val OPACITY_VALUES = listOf(0f, 1f, 0.9f, 1f, 0.95f, 1f, 1f)
}
