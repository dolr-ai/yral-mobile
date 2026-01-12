package com.yral.shared.libs.videoPlayer.cardstack

/**
 * Represents the direction of a swipe gesture on a card.
 */
enum class SwipeDirection {
    UP,
    DOWN,
    LEFT,
    RIGHT,
    NONE,
    ;

    companion object {
        /**
         * Determines the dominant swipe direction based on x and y offsets.
         * Returns NONE if offsets are too small to determine direction.
         */
        fun fromOffset(
            offsetX: Float,
            offsetY: Float,
        ): SwipeDirection {
            val absX = kotlin.math.abs(offsetX)
            val absY = kotlin.math.abs(offsetY)

            // Require minimum movement to determine direction
            val threshold = CardStackConstants.MIN_DIRECTION_THRESHOLD_PX
            if (absX < threshold && absY < threshold) return NONE

            return if (absX > absY) {
                if (offsetX > 0) RIGHT else LEFT
            } else {
                if (offsetY > 0) DOWN else UP
            }
        }
    }
}
