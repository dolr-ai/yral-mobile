package com.yral.shared.libs.videoplayback

internal data class FeedAlignmentPlan(
    val clearPlaybackState: Boolean,
    val nextActiveIndex: Int?,
    val invalidatePreparedIndex: Int?,
)

internal fun planFeedAlignment(
    previousIds: List<String>,
    currentIds: List<String>,
    activeIndex: Int,
    activeSlotIndex: Int?,
    preparedSlotIndex: Int?,
): FeedAlignmentPlan {
    if (currentIds.isEmpty()) {
        return FeedAlignmentPlan(
            clearPlaybackState = true,
            nextActiveIndex = null,
            invalidatePreparedIndex = preparedSlotIndex,
        )
    }

    val invalidatePreparedIndex =
        preparedSlotIndex?.takeIf { index ->
            index !in currentIds.indices || previousIds.getOrNull(index) != currentIds.getOrNull(index)
        }

    val nextActiveIndex =
        when {
            activeIndex !in currentIds.indices -> currentIds.lastIndex
            previousIds.getOrNull(activeIndex) != currentIds[activeIndex] -> activeIndex
            activeSlotIndex != activeIndex -> activeIndex
            else -> null
        }

    return FeedAlignmentPlan(
        clearPlaybackState = false,
        nextActiveIndex = nextActiveIndex,
        invalidatePreparedIndex = invalidatePreparedIndex,
    )
}
