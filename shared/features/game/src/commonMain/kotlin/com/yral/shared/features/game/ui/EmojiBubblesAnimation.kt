package com.yral.shared.features.game.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Constants for the emoji bubbles animation.
 * These values are tuned to match the visual style of the existing Lottie animations.
 */
private object EmojiBubblesConstants {
    const val NUM_EMOJIS = 10
    const val ANIMATION_DURATION_MS = 1800
    const val MAX_START_DELAY_MS = 200
    const val MIN_ALPHA = 0.2f
    const val MAX_ALPHA = 0.7f
    const val MIN_SCALE = 0.5f
    const val MAX_SCALE = 1.0f
    const val MIN_FONT_SIZE = 24
    const val MAX_FONT_SIZE = 50
    const val MIN_ROTATION = -40f
    const val MAX_ROTATION = 45f
    const val PADDING_RATIO = 0.1f
    const val START_Y_RATIO = 0.3f
    const val ALPHA_VARIANCE_START = 0.5f
    const val ALPHA_VARIANCE_END = 0.3f
    const val SCALE_VARIANCE_START = 0.5f
    const val SCALE_VARIANCE_END = 0.3f
}

/**
 * A Compose-based animation that renders multiple floating emoji characters
 * moving upward with rotation and opacity effects.
 *
 * This is used as a fallback for dynamic emojis that don't have pre-made Lottie animations.
 * The visual style mimics the existing smiley game Lottie animations (like fire, heart, etc.)
 *
 * @param emoji The unicode emoji character to animate
 * @param onAnimationComplete Called when all emoji animations have completed
 */
@Composable
fun EmojiBubblesAnimation(
    emoji: String,
    modifier: Modifier = Modifier,
    onAnimationComplete: () -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val containerWidthPx = constraints.maxWidth
        val containerHeightPx = constraints.maxHeight

        if (containerWidthPx == 0 || containerHeightPx == 0) return@BoxWithConstraints

        val emojiBubbles =
            remember(emoji, containerWidthPx, containerHeightPx) {
                mutableStateListOf<EmojiBubble>().apply {
                    repeat(EmojiBubblesConstants.NUM_EMOJIS) {
                        add(
                            EmojiBubble.create(
                                emoji = emoji,
                                containerWidth = containerWidthPx,
                                containerHeight = containerHeightPx,
                            ),
                        )
                    }
                }
            }

        Box(modifier = Modifier.fillMaxSize()) {
            emojiBubbles.forEach { bubble ->
                key(bubble.id) {
                    SingleEmojiBubble(
                        bubble = bubble,
                        containerHeight = containerHeightPx,
                        onAnimationEnd = {
                            emojiBubbles.remove(bubble)
                            if (emojiBubbles.isEmpty()) {
                                onAnimationComplete()
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SingleEmojiBubble(
    bubble: EmojiBubble,
    containerHeight: Int,
    onAnimationEnd: () -> Unit,
) {
    val progressAnim = remember { Animatable(0f) }
    val latestOnAnimationEnd by rememberUpdatedState(onAnimationEnd)

    LaunchedEffect(bubble.id) {
        delay(bubble.startDelay.toLong())
        progressAnim.animateTo(
            targetValue = 1f,
            animationSpec =
                tween(
                    durationMillis = bubble.duration,
                    easing = FastOutSlowInEasing,
                ),
        )
        latestOnAnimationEnd()
    }

    val progress = progressAnim.value

    // Calculate interpolated values
    val currentY = containerHeight * (1f - progress) - bubble.startY
    val currentAlpha = bubble.startAlpha + (bubble.endAlpha - bubble.startAlpha) * progress
    val currentRotation = bubble.startRotation + (bubble.endRotation - bubble.startRotation) * progress
    val currentScale = bubble.startScale + (bubble.endScale - bubble.startScale) * progress

    BasicText(
        text = bubble.emoji,
        style = TextStyle(fontSize = bubble.fontSize.sp),
        modifier =
            Modifier.graphicsLayer {
                translationX = bubble.xPosition
                translationY = currentY
                rotationZ = currentRotation
                alpha = currentAlpha
                scaleX = currentScale
                scaleY = currentScale
            },
    )
}

@OptIn(ExperimentalUuidApi::class)
private data class EmojiBubble(
    val id: String = Uuid.random().toString(),
    val emoji: String,
    val xPosition: Float,
    val startY: Float,
    val fontSize: Int,
    val startAlpha: Float,
    val endAlpha: Float,
    val startRotation: Float,
    val endRotation: Float,
    val startScale: Float,
    val endScale: Float,
    val duration: Int,
    val startDelay: Int,
) {
    companion object {
        fun create(
            emoji: String,
            containerWidth: Int,
            containerHeight: Int,
        ): EmojiBubble {
            val random = Random

            // Random horizontal position with some padding
            val padding = containerWidth * EmojiBubblesConstants.PADDING_RATIO
            val xPosition = padding + random.nextFloat() * (containerWidth - 2 * padding)

            // Random starting Y offset (spread out the start positions)
            val startY = random.nextFloat() * containerHeight * EmojiBubblesConstants.START_Y_RATIO

            // Random font size
            val fontSize =
                random.nextInt(
                    EmojiBubblesConstants.MIN_FONT_SIZE,
                    EmojiBubblesConstants.MAX_FONT_SIZE,
                )

            // Random alpha range (start lower, end higher for visibility during rise)
            val alphaRange = EmojiBubblesConstants.MAX_ALPHA - EmojiBubblesConstants.MIN_ALPHA
            val startAlpha =
                EmojiBubblesConstants.MIN_ALPHA +
                    random.nextFloat() * alphaRange * EmojiBubblesConstants.ALPHA_VARIANCE_START
            val endAlpha =
                EmojiBubblesConstants.MAX_ALPHA -
                    random.nextFloat() * alphaRange * EmojiBubblesConstants.ALPHA_VARIANCE_END

            // Random rotation
            val rotationRange = EmojiBubblesConstants.MAX_ROTATION - EmojiBubblesConstants.MIN_ROTATION
            val startRotation =
                EmojiBubblesConstants.MIN_ROTATION + random.nextFloat() * rotationRange
            val endRotation =
                EmojiBubblesConstants.MIN_ROTATION + random.nextFloat() * rotationRange

            // Random scale
            val scaleRange = EmojiBubblesConstants.MAX_SCALE - EmojiBubblesConstants.MIN_SCALE
            val startScale =
                EmojiBubblesConstants.MIN_SCALE +
                    random.nextFloat() * scaleRange * EmojiBubblesConstants.SCALE_VARIANCE_START
            val endScale =
                EmojiBubblesConstants.MAX_SCALE -
                    random.nextFloat() * scaleRange * EmojiBubblesConstants.SCALE_VARIANCE_END

            return EmojiBubble(
                emoji = emoji,
                xPosition = xPosition,
                startY = startY,
                fontSize = fontSize,
                startAlpha = startAlpha,
                endAlpha = endAlpha,
                startRotation = startRotation,
                endRotation = endRotation,
                startScale = startScale,
                endScale = endScale,
                duration = EmojiBubblesConstants.ANIMATION_DURATION_MS,
                startDelay = random.nextInt(0, EmojiBubblesConstants.MAX_START_DELAY_MS),
            )
        }
    }
}
