package com.yral.shared.libs.videoPlayer.cardstack

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Creates and remembers a [SwipeableCardState] for managing the card stack.
 *
 * @param initialIndex The starting index in the reels list.
 * @param itemCount Total number of items in the list.
 */
@Composable
fun rememberSwipeableCardState(
    initialIndex: Int = 0,
    itemCount: Int,
): SwipeableCardState =
    remember(itemCount) {
        SwipeableCardState(initialIndex, itemCount)
    }

/**
 * State holder for the swipeable card stack.
 * Manages card positions, animations, and gesture state.
 */
@Stable
class SwipeableCardState(
    initialIndex: Int,
    private var itemCount: Int,
) {
    /** Index closest to the snapped position (used for playback/active state) */
    var currentIndex by mutableIntStateOf(initialIndex)
        private set

    /** Index of the fully settled front card (used for rendering) */
    var settledIndex by mutableIntStateOf(initialIndex)
        private set

    /** Horizontal offset of the front card during drag */
    var offsetX by mutableFloatStateOf(0f)

    /** Vertical offset of the front card during drag */
    var offsetY by mutableFloatStateOf(0f)

    /** Rotation angle of the front card (based on horizontal drag) */
    var rotation by mutableFloatStateOf(0f)

    /** Whether the user is currently touching the screen */
    var isTouching by mutableStateOf(false)

    /** Whether the user is currently dragging the card */
    var isDragging by mutableStateOf(false)

    /** Current swipe direction based on drag offset */
    var swipeDirection by mutableStateOf(SwipeDirection.NONE)

    /** Whether a dismiss animation is currently running */
    var isAnimating by mutableStateOf(false)
        private set

    /** Whether the swipe has been committed (passed 50% threshold) - next video should start */
    var isSwipeCommitted by mutableStateOf(false)

    /** Animatable for smooth offset transitions */
    private val offsetAnimatable = Animatable(Offset.Zero, Offset.VectorConverter)

    /** Animatable for smooth rotation transitions */
    private val rotationAnimatable = Animatable(0f)

    /**
     * Updates the item count when the reels list changes.
     */
    fun updateItemCount(newCount: Int) {
        itemCount = newCount
        val maxIndex = (itemCount - 1).coerceAtLeast(0)
        settledIndex = settledIndex.coerceIn(0, maxIndex)
        currentIndex = currentIndex.coerceIn(0, maxIndex)
    }

    /**
     * Updates the drag offset and calculates rotation/direction.
     */
    fun updateDragOffset(
        dragAmount: Offset,
        screenWidth: Float,
    ) {
        offsetX += dragAmount.x
        offsetY += dragAmount.y
        rotation = (offsetX / screenWidth) * CardStackConstants.ROTATION_MULTIPLIER
        swipeDirection = SwipeDirection.fromOffset(offsetX, offsetY)
    }

    /**
     * Calculates the progress towards the swipe threshold (0.0 to 1.0+).
     */
    fun calculateSwipeProgress(
        screenWidth: Float,
        screenHeight: Float,
    ): Float {
        val thresholdX = screenWidth * CardStackConstants.SWIPE_THRESHOLD_FRACTION
        val thresholdY = screenHeight * CardStackConstants.SWIPE_THRESHOLD_FRACTION

        val progressX = abs(offsetX) / thresholdX
        val progressY = abs(offsetY) / thresholdY

        return maxOf(progressX, progressY)
    }

    /**
     * Checks if the current offset exceeds the dismiss threshold.
     */
    fun hasExceededThreshold(
        screenWidth: Float,
        screenHeight: Float,
    ): Boolean = calculateSwipeProgress(screenWidth, screenHeight) >= 1f

    /**
     * Updates the active index based on drag progress and commit threshold.
     * @return true if this call transitioned into the committed state.
     */
    fun updateCurrentIndexForDrag(
        screenWidth: Float,
        screenHeight: Float,
        commitThreshold: Float,
    ): Boolean {
        val shouldCommit =
            !isAtEnd() &&
                swipeDirection != SwipeDirection.NONE &&
                calculateSwipeProgress(screenWidth, screenHeight) >= commitThreshold
        val wasCommitted = isSwipeCommitted
        isSwipeCommitted = shouldCommit
        currentIndex = if (shouldCommit) nextIndex() else settledIndex
        return !wasCommitted && shouldCommit
    }

    /**
     * Forces the active index to the next card if possible.
     * @return true if the index moved to next.
     */
    fun commitToNext(): Boolean {
        if (isAtEnd()) {
            isSwipeCommitted = false
            currentIndex = settledIndex
            return false
        }
        isSwipeCommitted = true
        currentIndex = nextIndex()
        return true
    }

    /**
     * Animates the card dismissal in the current swipe direction.
     * @param screenWidth Width of the screen for calculating exit position.
     * @param screenHeight Height of the screen for calculating exit position.
     * @param onComplete Callback invoked after animation completes.
     */
    suspend fun animateDismiss(
        screenWidth: Float,
        screenHeight: Float,
        onComplete: () -> Unit,
    ) {
        isAnimating = true
        commitToNext()

        // Calculate exit position (off-screen in swipe direction)
        val exitMultiplier = CardStackConstants.EXIT_MULTIPLIER
        val targetOffset =
            when (swipeDirection) {
                SwipeDirection.LEFT -> Offset(-screenWidth * exitMultiplier, offsetY)
                SwipeDirection.RIGHT -> Offset(screenWidth * exitMultiplier, offsetY)
                SwipeDirection.UP -> Offset(offsetX, -screenHeight * exitMultiplier)
                SwipeDirection.DOWN -> Offset(offsetX, screenHeight * exitMultiplier)
                SwipeDirection.NONE -> Offset.Zero
            }

        // Calculate target rotation (angle based on direction)
        val targetRotation =
            when (swipeDirection) {
                SwipeDirection.LEFT -> -CardStackConstants.ROTATION_MULTIPLIER
                SwipeDirection.RIGHT -> CardStackConstants.ROTATION_MULTIPLIER
                else -> 0f
            }

        if (targetOffset != Offset.Zero) {
            offsetAnimatable.snapTo(Offset(offsetX, offsetY))
            rotationAnimatable.snapTo(rotation)

            // Animate both offset and rotation in parallel using coroutines
            coroutineScope {
                launch {
                    offsetAnimatable.animateTo(
                        targetValue = targetOffset,
                        animationSpec = tween(CardStackConstants.DISMISS_ANIMATION_DURATION_MS),
                    ) {
                        offsetX = value.x
                        offsetY = value.y
                    }
                }
                launch {
                    rotationAnimatable.animateTo(
                        targetValue = targetRotation,
                        animationSpec = tween(CardStackConstants.DISMISS_ANIMATION_DURATION_MS),
                    ) {
                        rotation = value
                    }
                }
            }
        }

        onComplete()
        advanceToNext()
        isAnimating = false
    }

    /**
     * Animates the card back to center position (snap back).
     */
    suspend fun snapBack() {
        isAnimating = true
        offsetAnimatable.snapTo(Offset(offsetX, offsetY))
        offsetAnimatable.animateTo(
            targetValue = Offset.Zero,
            animationSpec =
                spring(
                    dampingRatio = CardStackConstants.SNAP_BACK_DAMPING_RATIO,
                    stiffness = CardStackConstants.SNAP_BACK_STIFFNESS,
                ),
        ) {
            offsetX = value.x
            offsetY = value.y
            rotation = (offsetX / CardStackConstants.SNAP_BACK_STIFFNESS) * CardStackConstants.ROTATION_MULTIPLIER
        }
        swipeDirection = SwipeDirection.NONE
        isSwipeCommitted = false
        currentIndex = settledIndex
        isAnimating = false
    }

    /**
     * Advances to the next card in the stack.
     * Resets offset and rotation state.
     * @return true if there was a next card, false if at end.
     */
    fun advanceToNext(): Boolean =
        if (settledIndex < itemCount - 1) {
            settledIndex++
            currentIndex = settledIndex
            resetState()
            true
        } else {
            currentIndex = settledIndex
            resetState()
            false
        }

    /**
     * Checks if current index is at the last item.
     */
    fun isAtEnd(): Boolean = settledIndex >= itemCount - 1

    /**
     * Programmatically swipes the card in the specified direction.
     * Used for button-triggered swipes.
     *
     * @param direction The direction to swipe.
     * @param screenWidth Width of the screen for calculating exit position.
     * @param screenHeight Height of the screen for calculating exit position.
     * @param onComplete Callback invoked after animation completes.
     */
    suspend fun swipeInDirection(
        direction: SwipeDirection,
        screenWidth: Float,
        screenHeight: Float,
        onComplete: () -> Unit,
    ) {
        if (isAnimating || isAtEnd() || direction == SwipeDirection.NONE) return

        swipeDirection = direction
        commitToNext()
        animateDismiss(screenWidth, screenHeight, onComplete)
    }

    /**
     * Resets all drag/animation state to initial values.
     */
    private fun resetState() {
        offsetX = 0f
        offsetY = 0f
        rotation = 0f
        swipeDirection = SwipeDirection.NONE
        isTouching = false
        isDragging = false
        isSwipeCommitted = false
    }

    private fun nextIndex(): Int {
        val maxIndex = (itemCount - 1).coerceAtLeast(0)
        return (settledIndex + 1).coerceAtMost(maxIndex)
    }
}
