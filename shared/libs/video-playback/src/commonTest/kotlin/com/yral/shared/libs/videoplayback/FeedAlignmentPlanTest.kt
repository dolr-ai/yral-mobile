package com.yral.shared.libs.videoplayback

import kotlin.test.Test
import kotlin.test.assertEquals

class FeedAlignmentPlanTest {
    @Test
    fun `remove current item realigns active and invalidates prepared`() {
        val plan =
            planFeedAlignment(
                previousIds = listOf("a", "b", "c"),
                currentIds = listOf("b", "c"),
                activeIndex = 0,
                activeSlotIndex = 0,
                preparedSlotIndex = 1,
            )

        assertEquals(false, plan.clearPlaybackState)
        assertEquals(0, plan.nextActiveIndex)
        assertEquals(1, plan.invalidatePreparedIndex)
    }

    @Test
    fun `removal before active index clamps to last index`() {
        val plan =
            planFeedAlignment(
                previousIds = listOf("a", "b"),
                currentIds = listOf("a"),
                activeIndex = 1,
                activeSlotIndex = 1,
                preparedSlotIndex = null,
            )

        assertEquals(false, plan.clearPlaybackState)
        assertEquals(0, plan.nextActiveIndex)
        assertEquals(null, plan.invalidatePreparedIndex)
    }

    @Test
    fun `unchanged feed keeps alignment stable`() {
        val plan =
            planFeedAlignment(
                previousIds = listOf("a", "b", "c"),
                currentIds = listOf("a", "b", "c"),
                activeIndex = 1,
                activeSlotIndex = 1,
                preparedSlotIndex = 2,
            )

        assertEquals(false, plan.clearPlaybackState)
        assertEquals(null, plan.nextActiveIndex)
        assertEquals(null, plan.invalidatePreparedIndex)
    }

    @Test
    fun `empty feed clears playback state`() {
        val plan =
            planFeedAlignment(
                previousIds = listOf("a"),
                currentIds = emptyList(),
                activeIndex = 0,
                activeSlotIndex = 0,
                preparedSlotIndex = 1,
            )

        assertEquals(true, plan.clearPlaybackState)
        assertEquals(null, plan.nextActiveIndex)
        assertEquals(1, plan.invalidatePreparedIndex)
    }
}
