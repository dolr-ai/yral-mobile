package com.yral.shared.features.game.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.yral.shared.features.game.ui.CoinAnimationConstants.ANIMATION_DURATION
import com.yral.shared.features.game.ui.CoinAnimationConstants.HORIZONTAL_PADDING
import com.yral.shared.features.game.ui.CoinAnimationConstants.MAX_ALPHA
import com.yral.shared.features.game.ui.CoinAnimationConstants.MIN_ALPHA
import com.yral.shared.features.game.ui.CoinAnimationConstants.NUM_OF_TEXTS
import com.yral.shared.libs.designsystem.theme.kumbhSansFontFamily
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private object CoinAnimationConstants {
    const val NUM_OF_TEXTS = 1
    const val MIN_ALPHA = 0.6f
    const val MAX_ALPHA = 1.0f
    const val HORIZONTAL_PADDING = 16

    const val ANIMATION_DURATION = 2000
}

@Composable
fun CoinDeltaAnimation(
    text: String,
    textColor: Color = Color.White,
    modifier: Modifier = Modifier,
    onAnimationEnd: () -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        val containerWidthPx = constraints.maxWidth - HORIZONTAL_PADDING * 2
        // Use the real height of the container (constraints are in px)
        val containerHeightPx = constraints.maxHeight
        if (containerWidthPx == 0 || containerHeightPx == 0) return@BoxWithConstraints

        // Shared infinite alpha animation used by every floating text
        val infiniteTransition = rememberInfiniteTransition()
        val sharedAlpha by infiniteTransition.animateFloat(
            initialValue = MIN_ALPHA,
            targetValue = MAX_ALPHA,
            animationSpec =
                infiniteRepeatable(
                    animation =
                        tween(
                            durationMillis = ANIMATION_DURATION,
                            easing = LinearEasing,
                        ),
                ),
        )

        val fontFamily = kumbhSansFontFamily()
        // Use mutableStateListOf for recomposition on change
        val animatedTexts =
            remember(containerHeightPx) {
                mutableStateListOf<AnimatedText>().apply {
                    repeat(NUM_OF_TEXTS) {
                        add(AnimatedText.create(containerHeightPx, text, textColor, fontFamily))
                    }
                }
            }

        Box(modifier = Modifier.fillMaxSize()) {
            animatedTexts.forEach { animatedText ->
                key(animatedText.id) {
                    SingleAnimatedText(
                        containerHeightPx = containerHeightPx,
                        containerWidthPx = containerWidthPx,
                        animatedText = animatedText,
                        animatedAlpha = sharedAlpha,
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
}

@Composable
fun SingleAnimatedText(
    containerHeightPx: Int,
    containerWidthPx: Int,
    animatedText: AnimatedText,
    animatedAlpha: Float,
    onAnimationEnd: () -> Unit,
) {
    val anim = remember { Animatable(animatedText.startY.toFloat()) }
    val latestOnAnimationEnd by rememberUpdatedState(onAnimationEnd)
    LaunchedEffect(Unit) {
        delay(animatedText.delay.toLong())
        anim.animateTo(
            targetValue = animatedText.endY.toFloat(),
            animationSpec =
                tween(
                    durationMillis = animatedText.duration,
                    easing = FastOutLinearInEasing,
                ),
        )
        latestOnAnimationEnd()
    }
    var textWidth by remember { mutableIntStateOf(0) }
    Text(
        text = animatedText.text,
        fontFamily = animatedText.fontFamily,
        fontSize = animatedText.fontSize,
        fontWeight = animatedText.fontWeight,
        color = animatedText.textColor,
        modifier =
            Modifier
                .onSizeChanged {
                    textWidth = it.width
                }.graphicsLayer {
                    translationX = ((containerWidthPx - textWidth) / 2f)
                    translationY = (containerHeightPx.toFloat() - anim.value)
                    rotationZ = animatedText.rotationDegrees
                    alpha = animatedAlpha
                },
    )
}

@OptIn(ExperimentalUuidApi::class)
data class AnimatedText(
    val id: String = Uuid.random().toString(),
    val text: String,
    val textColor: Color,
    val fontFamily: FontFamily,
    val fontSize: TextUnit = FONT_SIZE.sp,
    val fontWeight: FontWeight = FONT_WEIGHT,
    val rotationDegrees: Float = TILT_ANGLE,
    val duration: Int = ANIMATION_DURATION,
    val delay: Int = Random.nextInt(from = 0, until = TEXT_START_MAX_DELAY),
    val startY: Int,
    val endY: Int,
) {
    companion object {
        private const val TILT_ANGLE = 5f
        private const val TEXT_START_MAX_DELAY = 300
        private const val FONT_SIZE = 64
        private val FONT_WEIGHT = FontWeight.Black

        fun create(
            screenHeight: Int,
            text: String,
            textColor: Color,
            fontFamily: FontFamily,
        ): AnimatedText =
            AnimatedText(
                text = text,
                textColor = textColor,
                fontFamily = fontFamily,
                startY = 0,
                endY = screenHeight,
            )
    }
}
