package com.yral.android.ui.screens.home.feed

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.yral.android.ui.screens.home.feed.BubbleAnimationConstants.ANIMATION_DURATION
import com.yral.android.ui.screens.home.feed.BubbleAnimationConstants.BUBBLE_START_DELAY
import com.yral.android.ui.screens.home.feed.BubbleAnimationConstants.HORIZONTAL_PADDING
import com.yral.android.ui.screens.home.feed.BubbleAnimationConstants.MAX_ALPHA
import com.yral.android.ui.screens.home.feed.BubbleAnimationConstants.MAX_BUBBLE_SIZE
import com.yral.android.ui.screens.home.feed.BubbleAnimationConstants.MIN_ALPHA
import com.yral.android.ui.screens.home.feed.BubbleAnimationConstants.MIN_BUBBLE_SIZE
import com.yral.android.ui.screens.home.feed.BubbleAnimationConstants.NUM_OF_BUBBLES
import com.yral.android.ui.screens.home.feed.BubbleAnimationConstants.TILT_ANGLE
import kotlinx.coroutines.delay
import java.util.UUID
import kotlin.random.Random

private object BubbleAnimationConstants {
    const val NUM_OF_BUBBLES = 10
    const val ANIMATION_DURATION = 1500
    const val MIN_ALPHA = 0.3f
    const val MAX_ALPHA = 0.8f
    const val BUBBLE_START_DELAY = 300
    const val MIN_BUBBLE_SIZE = 26
    const val MAX_BUBBLE_SIZE = 38
    const val TILT_ANGLE = 100f
    const val HORIZONTAL_PADDING = 16
}

@Composable
fun BubbleAnimation(
    resourceId: Int,
    onAnimationEnd: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val screenWidthPx =
        with(density) {
            (configuration.screenWidthDp.dp - HORIZONTAL_PADDING.dp * 2).toPx()
        }
    // Use mutableStateListOf for recomposition on change
    val bubbles =
        remember {
            mutableStateListOf<Bubble>().apply {
                repeat(NUM_OF_BUBBLES) {
                    add(Bubble.create(screenHeightPx, screenWidthPx))
                }
            }
        }
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
    ) {
        bubbles.forEach { bubble ->
            key(bubble.id) {
                SingleBubble(
                    resourceId = resourceId,
                    screenHeightPx = screenHeightPx,
                    bubble = bubble,
                ) {
                    bubbles.remove(bubble)
                    if (bubbles.isEmpty()) {
                        onAnimationEnd()
                    }
                }
            }
        }
    }
}

@Composable
fun SingleBubble(
    resourceId: Int,
    screenHeightPx: Float,
    bubble: Bubble,
    onAnimationEnd: () -> Unit,
) {
    val anim = remember { Animatable(bubble.startY) }
    LaunchedEffect(Unit) {
        delay(bubble.delay.toLong())
        anim.animateTo(
            targetValue = bubble.endY,
            animationSpec =
                tween(
                    durationMillis = bubble.duration,
                    easing = FastOutLinearInEasing,
                ),
        )
        onAnimationEnd()
    }
    val alphaAnim = rememberInfiniteTransition()
    val alpha by alphaAnim.animateFloat(
        initialValue = MIN_ALPHA,
        targetValue = MAX_ALPHA,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        durationMillis = bubble.duration,
                        easing = LinearEasing,
                    ),
            ),
    )
    Image(
        painter = painterResource(id = resourceId),
        contentDescription = "Bubble Image",
        contentScale = ContentScale.Fit,
        modifier =
            Modifier
                .offset {
                    IntOffset(
                        x = bubble.xPosition.toInt(),
                        y = (screenHeightPx - anim.value).toInt(),
                    )
                }.size(bubble.radius.dp)
                .graphicsLayer(
                    rotationZ = bubble.rotationDegrees,
                    alpha = alpha,
                ),
    )
}

@Suppress("MagicNumber")
data class Bubble(
    val id: String = UUID.randomUUID().toString(),
    val radius: Int = Random.nextInt(MIN_BUBBLE_SIZE, MAX_BUBBLE_SIZE),
    val xPosition: Float,
    val startY: Float,
    val endY: Float,
    val rotationDegrees: Float = (Random.nextFloat() - 0.5f) * TILT_ANGLE,
    val duration: Int = ANIMATION_DURATION,
    val delay: Int = Random.nextInt(0, BUBBLE_START_DELAY),
) {
    companion object {
        fun create(
            screenHeight: Float,
            screenWidth: Float,
        ): Bubble {
            // Generate a number between -1 and 1
            val centerBiased = (Random.nextFloat() - 0.5f) * 2f
            // Invert the curve to bias toward center: y = 1 - x^2
            val bias = 1f - centerBiased * centerBiased
            // Mixing factor: 0.0 = full uniform, 1.0 = fully center-biased
            val mixing = 0.1f // <-- you can tweak this between 0.0 and 1.0

            // Interpolate between uniform and biased
            val baseX = Random.nextFloat() * screenWidth
            val biasedX = (screenWidth / 2f) + centerBiased * bias * (screenWidth / 2f)
            val xPosition = baseX * (1f - mixing) + biasedX * mixing

            return Bubble(
                xPosition = xPosition,
                startY = 0f,
                endY = screenHeight,
            )
        }
    }
}
