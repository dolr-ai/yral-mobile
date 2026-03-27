package com.yral.shared.core.videostate

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoGenerationTrackerTest {
    @AfterTest
    fun tearDown() {
        VideoGenerationTracker.stopGenerating()
        VideoGenerationTracker.consumeDraftsTabRequest()
        VideoGenerationTracker.state.value.draftVideoIds.forEach(VideoGenerationTracker::clearDraft)
    }

    @Test
    fun `start and stop generation update tracker state`() {
        VideoGenerationTracker.startGenerating()
        VideoGenerationTracker.updateProgress(1.5f)

        assertTrue(VideoGenerationTracker.state.value.isGenerating)
        assertEquals(1f, VideoGenerationTracker.state.value.progress)

        VideoGenerationTracker.stopGenerating()

        assertFalse(VideoGenerationTracker.state.value.isGenerating)
        assertEquals(0f, VideoGenerationTracker.state.value.progress)
    }

    @Test
    fun `draft selection request can be consumed`() {
        VideoGenerationTracker.requestDraftsTab()

        assertTrue(VideoGenerationTracker.selectDraftsTab.value)

        VideoGenerationTracker.consumeDraftsTabRequest()

        assertFalse(VideoGenerationTracker.selectDraftsTab.value)
    }

    @Test
    fun `marking and clearing draft updates tracked ids`() {
        VideoGenerationTracker.markAsDraft("video-1")

        assertEquals(setOf("video-1"), VideoGenerationTracker.state.value.draftVideoIds)

        VideoGenerationTracker.clearDraft("video-1")

        assertTrue(VideoGenerationTracker.state.value.draftVideoIds.isEmpty())
    }
}
