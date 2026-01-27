package com.yral.shared.libs.videoPlayer.cardstack

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier

/**
 * Generic swipeable card stack that manages gestures, animations, and layout.
 * Rendering of each card is delegated to [content], similar to pager APIs.
 */
@Composable
internal fun SwipeableCardStack(
    modifier: Modifier = Modifier,
    state: SwipeableCardState,
    count: Int,
    maxVisibleCards: Int = CardStackConstants.VISIBLE_CARDS,
    verticalSwipeEnabled: Boolean = true,
    key: ((index: Int) -> Any)? = null,
    onSwipeCommitted: (SwipeDirection) -> Unit = {},
    onSwipeComplete: (SwipeDirection) -> Unit = {},
    onEdgeReached: (SwipeDirection) -> Unit = {},
    content: @Composable SwipeableCardStackItemScope.() -> Unit,
) {
    if (count <= 0) return

    LaunchedEffect(count) {
        state.updateItemCount(count)
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidth = constraints.maxWidth.toFloat()
        val screenHeight = constraints.maxHeight.toFloat()
        val isTransitioning = state.currentIndex != state.settledIndex

        val baseIndex = state.currentIndex
        val remaining = (count - baseIndex).coerceAtLeast(0)
        val visibleCardCount = minOf(maxVisibleCards, remaining)

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .swipeableCard(
                        state = state,
                        enabled = !state.isAnimating && visibleCardCount > 0,
                        verticalSwipeEnabled = verticalSwipeEnabled,
                        onSwipeCommitted = onSwipeCommitted,
                        onSwipeComplete = onSwipeComplete,
                        onEdgeReached = onEdgeReached,
                    ),
        ) {
            for (stackOffset in (visibleCardCount - 1) downTo 0) {
                val index = baseIndex + stackOffset
                if (index >= count) continue

                val itemKey = key?.invoke(index) ?: index
                key(itemKey) {
                    SwipeableCardStackItem(
                        stackIndex = stackOffset,
                        state = state,
                        screenWidth = screenWidth,
                        screenHeight = screenHeight,
                        applyFrontTransform = !(isTransitioning && stackOffset == 0),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        SwipeableCardStackItemScope(
                            state = state,
                            index = index,
                            stackIndex = stackOffset,
                            screenWidth = screenWidth,
                            screenHeight = screenHeight,
                        ).content()
                    }
                }
            }
        }
    }
}

@Stable
internal class SwipeableCardStackItemScope internal constructor(
    val state: SwipeableCardState,
    val index: Int,
    val stackIndex: Int,
    val screenWidth: Float,
    val screenHeight: Float,
) {
    val isFrontCard: Boolean
        get() = stackIndex == 0

    val isNextCard: Boolean
        get() = stackIndex == 1

    val swipeDirection: SwipeDirection
        get() = state.swipeDirection

    val swipeProgress: Float
        get() = state.calculateSwipeProgress(screenWidth, screenHeight)
}
