package com.yral.shared.libs.videoPlayer.cardstack

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import yral_mobile.shared.libs.videoplayer.generated.resources.Res
import yral_mobile.shared.libs.videoplayer.generated.resources.flop_icon
import yral_mobile.shared.libs.videoplayer.generated.resources.hit_icon

private const val BASE_BUTTON_SIZE = 52f
private const val MAX_SCALE = 1.4f
private const val BUTTON_OFFSET = 40f // Half of 80dp spacing
private const val FADE_ANIMATION_DURATION = 200 // ms

/**
 * Swipe action buttons for the card stack.
 * Shows FLOP button on left (triggers left swipe) and HIT button on right (triggers right swipe).
 *
 * Layout:
 * - Fixed positions: buttons stay in place even when one is hidden
 * - 80dp spacing between buttons (40dp offset each from center)
 *
 * When swiping:
 * - Swiping RIGHT: Like button scales up in place, close button hides
 * - Swiping LEFT: Close button scales up in place, like button hides
 *
 * @param onFlopClick Callback when FLOP button is clicked (trigger left swipe).
 * @param onHitClick Callback when HIT button is clicked (trigger right swipe).
 * @param swipeDirection Current swipe direction.
 * @param swipeProgress Progress of swipe (0 to 1+).
 * @param modifier Modifier for the container.
 */
@Suppress("LongMethod")
@Composable
fun SwipeButtons(
    onFlopClick: () -> Unit,
    onHitClick: () -> Unit,
    swipeDirection: SwipeDirection = SwipeDirection.NONE,
    swipeProgress: Float = 0f,
    modifier: Modifier = Modifier,
) {
    val flopInteractionSource = remember { MutableInteractionSource() }
    val hitInteractionSource = remember { MutableInteractionSource() }

    // Target visibility based on swipe direction
    val flopTargetAlpha = if (swipeDirection == SwipeDirection.RIGHT) 0f else 1f
    val hitTargetAlpha = if (swipeDirection == SwipeDirection.LEFT) 0f else 1f

    // Animate alpha for smooth fade in/out
    val flopAlpha =
        animateFloatAsState(
            targetValue = flopTargetAlpha,
            animationSpec = tween(durationMillis = FADE_ANIMATION_DURATION),
            label = "flopAlpha",
        )
    val hitAlpha =
        animateFloatAsState(
            targetValue = hitTargetAlpha,
            animationSpec = tween(durationMillis = FADE_ANIMATION_DURATION),
            label = "hitAlpha",
        )

    // Scale up the active button based on progress
    val flopScale =
        if (swipeDirection == SwipeDirection.LEFT) {
            1f + (swipeProgress.coerceIn(0f, 1f) * (MAX_SCALE - 1f))
        } else {
            1f
        }
    val hitScale =
        if (swipeDirection == SwipeDirection.RIGHT) {
            1f + (swipeProgress.coerceIn(0f, 1f) * (MAX_SCALE - 1f))
        } else {
            1f
        }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        // FLOP button (left) - fixed position offset to the left
        Image(
            painter = painterResource(Res.drawable.flop_icon),
            contentDescription = "Flop - Swipe Left",
            modifier =
                Modifier
                    .offset(x = (-BUTTON_OFFSET).dp)
                    .size(BASE_BUTTON_SIZE.dp)
                    .alpha(flopAlpha.value)
                    .graphicsLayer {
                        scaleX = flopScale
                        scaleY = flopScale
                    }.clickable(
                        interactionSource = flopInteractionSource,
                        indication = null,
                        enabled = flopAlpha.value > 0.5f,
                        onClick = onFlopClick,
                    ),
        )

        // HIT button (right) - fixed position offset to the right
        Image(
            painter = painterResource(Res.drawable.hit_icon),
            contentDescription = "Hit - Swipe Right",
            modifier =
                Modifier
                    .offset(x = BUTTON_OFFSET.dp)
                    .size(BASE_BUTTON_SIZE.dp)
                    .alpha(hitAlpha.value)
                    .graphicsLayer {
                        scaleX = hitScale
                        scaleY = hitScale
                    }.clickable(
                        interactionSource = hitInteractionSource,
                        indication = null,
                        enabled = hitAlpha.value > 0.5f,
                        onClick = onHitClick,
                    ),
        )
    }
}
