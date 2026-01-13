@file:Suppress("MagicNumber")

package com.yral.shared.libs.videoPlayer.cardstack

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Local color definitions for swipe feedback.
 * These mirror the YRAL design system colors.
 */
private object SwipeFeedbackColors {
    val Pink300 = Color(0xFFE2017B)
    val Yellow200 = Color(0xFFFFC33A)
    val Red300 = Color(0xFFF14331)
    val Green300 = Color(0xFF1EC981)
}

/**
 * Overlay that shows color feedback during swipe gestures.
 * The color and intensity indicate the swipe direction and progress.
 *
 * @param direction Current swipe direction.
 * @param progress Progress towards swipe threshold (0.0 to 1.0+).
 * @param modifier Modifier for the overlay.
 */
@Composable
fun SwipeFeedbackOverlay(
    direction: SwipeDirection,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val isVisible = direction != SwipeDirection.NONE && progress > 0.1f

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        val alpha = (progress.coerceIn(0f, 1f) * CardStackConstants.SWIPE_FEEDBACK_MAX_ALPHA)
        val color = getSwipeColor(direction).copy(alpha = alpha)

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(color),
        )
    }
}

/**
 * Returns the color associated with a swipe direction.
 */
private fun getSwipeColor(direction: SwipeDirection): Color =
    when (direction) {
        SwipeDirection.UP -> SwipeFeedbackColors.Pink300
        SwipeDirection.DOWN -> SwipeFeedbackColors.Yellow200
        SwipeDirection.LEFT -> SwipeFeedbackColors.Red300
        SwipeDirection.RIGHT -> SwipeFeedbackColors.Green300
        SwipeDirection.NONE -> Color.Transparent
    }
