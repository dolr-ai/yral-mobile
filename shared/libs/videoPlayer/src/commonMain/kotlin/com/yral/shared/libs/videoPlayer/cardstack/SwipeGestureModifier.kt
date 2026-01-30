package com.yral.shared.libs.videoPlayer.cardstack

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Modifier that enables swipeable card gesture detection.
 *
 * @param state The [SwipeableCardState] to update during gestures.
 * @param enabled Whether gestures are enabled.
 * @param verticalSwipeEnabled Whether vertical (up/down) swipes are allowed.
 * @param commitThreshold Fraction of swipe threshold at which to commit (0.5 = 50% of threshold).
 * @param onSwipeCommitted Callback invoked when drag exceeds commitThreshold (during drag, before release).
 * @param onSwipeComplete Callback invoked when a card dismiss animation finishes.
 * @param onEdgeReached Callback invoked when user tries to swipe at the last card.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod", "ComplexCondition")
fun Modifier.swipeableCard(
    state: SwipeableCardState,
    enabled: Boolean = true,
    verticalSwipeEnabled: Boolean = true,
    commitThreshold: Float = CardStackConstants.SWIPE_COMMIT_THRESHOLD,
    onSwipeCommitted: (SwipeDirection) -> Unit = {},
    onSwipeComplete: (SwipeDirection) -> Unit,
    onEdgeReached: (SwipeDirection) -> Unit = {},
): Modifier =
    composed {
        val coroutineScope = rememberCoroutineScope()
        val velocityTracker = VelocityTracker()

        if (!enabled || state.isAnimating) {
            return@composed this
        }

        this.pointerInput(state, enabled) {
            val screenWidth = size.width.toFloat()
            val screenHeight = size.height.toFloat()

            awaitEachGesture {
                // Pause video immediately on touch down
                val down = awaitFirstDown(requireUnconsumed = false)
                state.isTouching = true
                state.isDragging = true
                velocityTracker.resetTracking()
                velocityTracker.addPosition(down.uptimeMillis, down.position)

                var lastChange: PointerInputChange? = down
                var hasCommittedThisGesture = false

                // Track drag movements
                @Suppress("LoopWithTooManyJumpStatements")
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break

                    if (!change.pressed) {
                        // Finger lifted
                        lastChange = change
                        break
                    }

                    velocityTracker.addPosition(change.uptimeMillis, change.position)

                    val dragAmount = change.positionChange()
                    if (dragAmount != Offset.Zero) {
                        change.consume()
                        state.updateDragOffset(
                            dragAmount = dragAmount,
                            screenWidth = screenWidth,
                        )

                        // Update active index based on drag progress.
                        val didCommit =
                            state.updateCurrentIndexForDrag(
                                screenWidth = screenWidth,
                                screenHeight = screenHeight,
                                commitThreshold = commitThreshold,
                            )
                        if (didCommit && !hasCommittedThisGesture) {
                            hasCommittedThisGesture = true
                            onSwipeCommitted(state.swipeDirection)
                        }
                    }

                    lastChange = change
                }

                // Touch ended
                state.isTouching = false
                state.isDragging = false

                val direction = state.swipeDirection

                // Check if threshold exceeded or fling velocity is high enough
                val velocity = velocityTracker.calculateVelocity()
                val velocityMagnitude = maxOf(abs(velocity.x), abs(velocity.y))
                val exceededThreshold = state.hasExceededThreshold(screenWidth, screenHeight)
                val isFling = velocityMagnitude > CardStackConstants.FLING_VELOCITY_THRESHOLD

                // Check if vertical swipe is allowed
                val isVerticalSwipe = direction.isVertical()
                val isSwipeAllowed = !isVerticalSwipe || verticalSwipeEnabled

                if ((exceededThreshold || isFling) && direction != SwipeDirection.NONE && isSwipeAllowed) {
                    // Check if at end of list
                    if (state.isAtEnd()) {
                        onEdgeReached(direction)
                        coroutineScope.launch {
                            state.snapBack()
                        }
                    } else {
                        // Dismiss the card
                        // Fire commit callback if not already fired during drag (e.g., for flings)
                        if (!hasCommittedThisGesture) {
                            state.commitToNext()
                            onSwipeCommitted(direction)
                        }
                        coroutineScope.launch {
                            state.animateDismiss(
                                screenWidth = screenWidth,
                                screenHeight = screenHeight,
                                onComplete = { onSwipeComplete(direction) },
                            )
                        }
                    }
                } else {
                    // Snap back to center
                    coroutineScope.launch {
                        state.snapBack()
                    }
                }
            }
        }
    }

/**
 * Extension to calculate if this is primarily a horizontal or vertical swipe.
 */
fun SwipeDirection.isHorizontal(): Boolean = this == SwipeDirection.LEFT || this == SwipeDirection.RIGHT

/**
 * Extension to calculate if this is primarily a vertical swipe.
 */
fun SwipeDirection.isVertical(): Boolean = this == SwipeDirection.UP || this == SwipeDirection.DOWN
