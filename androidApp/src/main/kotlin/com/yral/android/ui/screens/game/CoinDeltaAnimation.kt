package com.yral.android.ui.screens.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yral.android.ui.design.YralTypoGraphy
import com.yral.android.ui.screens.game.CoinAnimationConstants.FONT_SIZE
import com.yral.android.ui.screens.game.CoinAnimationConstants.FONT_WIGHT
import kotlinx.coroutines.delay
import java.util.UUID
import kotlin.random.Random

private object CoinAnimationConstants {
    const val NUM_OF_TEXTS = 1
    const val ANIMATION_DURATION = 2000
    const val MIN_ALPHA = 0.6f
    const val MAX_ALPHA = 1.0f
    const val TEXT_START_DELAY = 300
    const val TILT_ANGLE = 10f
    const val HORIZONTAL_PADDING = 16
    const val FONT_SIZE = 64
    val FONT_WIGHT = FontWeight.Black
}

@Composable
fun CoinDeltaAnimation(
    text: String,
    textColor: Color = Color.White,
    onAnimationEnd: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val screenWidthPx =
        with(density) {
            (configuration.screenWidthDp.dp - CoinAnimationConstants.HORIZONTAL_PADDING.dp * 2).toPx()
        }

    // Calculate center of screen width
    val centerX = screenWidthPx / 2

    // Use mutableStateListOf for recomposition on change
    val animatedTexts =
        remember {
            mutableStateListOf<AnimatedText>().apply {
                repeat(CoinAnimationConstants.NUM_OF_TEXTS) {
                    add(AnimatedText.create(screenHeightPx, centerX, text))
                }
            }
        }

    Box(
        modifier =
            Modifier.fillMaxSize(),
    ) {
        animatedTexts.forEach { animatedText ->
            key(animatedText.id) {
                SingleAnimatedText(
                    text = animatedText.text,
                    textColor = textColor,
                    screenHeightPx = screenHeightPx,
                    animatedText = animatedText,
                ) {
                    animatedTexts.remove(animatedText)
                    if (animatedTexts.isEmpty()) {
                        onAnimationEnd()
                    }
                }
            }
        }
    }
}

@Composable
fun SingleAnimatedText(
    text: String,
    textColor: Color,
    screenHeightPx: Float,
    animatedText: AnimatedText,
    onAnimationEnd: () -> Unit,
) {
    val anim = remember { Animatable(animatedText.startY) }

    LaunchedEffect(Unit) {
        delay(animatedText.delay.toLong())
        anim.animateTo(
            targetValue = animatedText.endY,
            animationSpec =
                tween(
                    durationMillis = animatedText.duration,
                    easing = FastOutLinearInEasing,
                ),
        )
        onAnimationEnd()
    }

    val alphaAnim = rememberInfiniteTransition()
    val alpha by alphaAnim.animateFloat(
        initialValue = CoinAnimationConstants.MIN_ALPHA,
        targetValue = CoinAnimationConstants.MAX_ALPHA,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        durationMillis = animatedText.duration,
                        easing = LinearEasing,
                    ),
            ),
    )
    Text(
        text = text,
        fontFamily = YralTypoGraphy.KumbhSans,
        fontSize = FONT_SIZE.sp,
        fontWeight = FONT_WIGHT,
        color = textColor,
        modifier =
            Modifier
                .offset {
                    IntOffset(
                        x = animatedText.xPosition.toInt(),
                        y = (screenHeightPx - anim.value).toInt(),
                    )
                }.graphicsLayer(
                    rotationZ = animatedText.rotationDegrees,
                    alpha = alpha,
                ),
    )
}

@Suppress("MagicNumber")
data class AnimatedText(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val xPosition: Float,
    val startY: Float,
    val endY: Float,
    val rotationDegrees: Float = (Random.nextFloat() - 0.5f) * CoinAnimationConstants.TILT_ANGLE,
    val duration: Int = CoinAnimationConstants.ANIMATION_DURATION,
    val delay: Int = Random.nextInt(0, CoinAnimationConstants.TEXT_START_DELAY),
) {
    companion object {
        fun create(
            screenHeight: Float,
            centerX: Float,
            text: String,
        ): AnimatedText =
            AnimatedText(
                text = text,
                xPosition = centerX,
                startY = 0f,
                endY = screenHeight,
            )
    }
}
