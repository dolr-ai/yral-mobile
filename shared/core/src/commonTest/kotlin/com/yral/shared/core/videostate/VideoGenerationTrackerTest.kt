package com.yral.shared.core.videostate

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoGenerationTrackerTest {
    @AfterTest
    fun tearDown() {
        VideoGenerationTracker.clearPendingGenerations()
        VideoGenerationTracker.consumeDraftsTabRequest()
    }

    @Test
    fun `start and stop generation update tracker state`() {
        VideoGenerationTracker.startGenerating()
        val pendingGenerationId = VideoGenerationTracker.state.value.pendingGenerations.single().id
        VideoGenerationTracker.updateProgress(pendingGenerationId = pendingGenerationId, progress = 1.5f)

        assertTrue(VideoGenerationTracker.state.value.isGenerating)
        assertEquals(1, VideoGenerationTracker.state.value.pendingGenerations.size)
        assertEquals(1f, VideoGenerationTracker.state.value.pendingGenerations.single().progress)

        VideoGenerationTracker.stopGenerating()

        assertFalse(VideoGenerationTracker.state.value.isGenerating)
        assertTrue(VideoGenerationTracker.state.value.pendingGenerations.isEmpty())
    }

    @Test
    fun `draft created removes oldest pending generation`() {
        VideoGenerationTracker.startGenerating()
        VideoGenerationTracker.startGenerating()

        val oldestPendingGenerationId = VideoGenerationTracker.state.value.pendingGenerations.first().id
        val remainingPendingGenerationId = VideoGenerationTracker.state.value.pendingGenerations.last().id

        VideoGenerationTracker.onDraftCreated()

        assertTrue(VideoGenerationTracker.state.value.isGenerating)
        assertEquals(1, VideoGenerationTracker.state.value.pendingGenerations.size)
        assertFalse(
            VideoGenerationTracker.state.value.pendingGenerations.any { it.id == oldestPendingGenerationId },
        )
        assertEquals(remainingPendingGenerationId, VideoGenerationTracker.state.value.pendingGenerations.single().id)
    }

    @Test
    fun `stop generating removes most recent pending generation`() {
        VideoGenerationTracker.startGenerating()
        VideoGenerationTracker.startGenerating()

        val oldestPendingGenerationId = VideoGenerationTracker.state.value.pendingGenerations.first().id
        val newestPendingGenerationId = VideoGenerationTracker.state.value.pendingGenerations.last().id

        VideoGenerationTracker.stopGenerating()

        assertTrue(VideoGenerationTracker.state.value.isGenerating)
        assertEquals(1, VideoGenerationTracker.state.value.pendingGenerations.size)
        assertFalse(
            VideoGenerationTracker.state.value.pendingGenerations.any { it.id == newestPendingGenerationId },
        )
        assertEquals(oldestPendingGenerationId, VideoGenerationTracker.state.value.pendingGenerations.single().id)
    }

    @Test
    fun `draft selection request can be consumed`() {
        VideoGenerationTracker.requestDraftsTab()

        assertTrue(VideoGenerationTracker.selectDraftsTab.value)

        VideoGenerationTracker.consumeDraftsTabRequest()

        assertFalse(VideoGenerationTracker.selectDraftsTab.value)
    }

    @Test
    fun `draft created and request drafts tab updates both states together`() {
        VideoGenerationTracker.startGenerating()

        VideoGenerationTracker.onDraftCreatedAndRequestDraftsTab()

        assertFalse(VideoGenerationTracker.state.value.isGenerating)
        assertTrue(VideoGenerationTracker.selectDraftsTab.value)
    }

    @Test
    fun `clear pending generations removes all pending items`() {
        VideoGenerationTracker.startGenerating()
        VideoGenerationTracker.startGenerating()

        VideoGenerationTracker.clearPendingGenerations()

        assertFalse(VideoGenerationTracker.state.value.isGenerating)
        assertTrue(VideoGenerationTracker.state.value.pendingGenerations.isEmpty())
    }

    @Test
    fun `generating progress target starts at zero and caps at ninety five percent`() {
        assertEquals(0f, VideoGenerationTracker.generatingProgressTargetForElapsed(0))
        assertEquals(
            VideoGenerationTracker.ANIMATION_MAX_PROGRESS,
            VideoGenerationTracker.generatingProgressTargetForElapsed(60_000),
        )
        assertEquals(
            VideoGenerationTracker.ANIMATION_MAX_PROGRESS,
            VideoGenerationTracker.generatingProgressTargetForElapsed(90_000),
        )
    }

    @Test
    fun `generating progress target moves faster at the start than the end`() {
        val firstQuarterDelta =
            VideoGenerationTracker.generatingProgressTargetForElapsed(15_000) -
                VideoGenerationTracker.generatingProgressTargetForElapsed(0)
        val lastQuarterDelta =
            VideoGenerationTracker.generatingProgressTargetForElapsed(60_000) -
                VideoGenerationTracker.generatingProgressTargetForElapsed(45_000)

        assertTrue(firstQuarterDelta > lastQuarterDelta)
    }

    @Test
    fun `displayed generating progress uses the higher progress and stays capped`() {
        assertEquals(
            0.5f,
            VideoGenerationTracker.displayedGeneratingProgress(
                animatedProgress = 0.2f,
                reportedProgress = 0.5f,
            ),
        )
        assertEquals(
            0.6f,
            VideoGenerationTracker.displayedGeneratingProgress(
                animatedProgress = 0.6f,
                reportedProgress = 0.4f,
            ),
        )
        assertEquals(
            VideoGenerationTracker.ANIMATION_MAX_PROGRESS,
            VideoGenerationTracker.displayedGeneratingProgress(
                animatedProgress = 1.2f,
                reportedProgress = 0.98f,
            ),
        )
    }

    @Test
    fun `update progress only updates matching pending generation`() {
        VideoGenerationTracker.startGenerating()
        VideoGenerationTracker.startGenerating()
        val firstPendingGeneration = VideoGenerationTracker.state.value.pendingGenerations.first()
        val secondPendingGeneration = VideoGenerationTracker.state.value.pendingGenerations.last()

        VideoGenerationTracker.updateProgress(
            pendingGenerationId = firstPendingGeneration.id,
            progress = 0.4f,
        )

        assertEquals(0.4f, VideoGenerationTracker.state.value.pendingGenerations.first().progress)
        assertEquals(secondPendingGeneration.progress, VideoGenerationTracker.state.value.pendingGenerations.last().progress)
    }

    @Test
    fun `elapsed millis for progress maps back to generating target`() {
        val progress = 0.53f

        val elapsedMillis = VideoGenerationTracker.elapsedMillisForProgress(progress)
        val mappedProgress = VideoGenerationTracker.generatingProgressTargetForElapsed(elapsedMillis)

        assertTrue(mappedProgress >= progress - 0.01f)
        assertTrue(mappedProgress <= progress + 0.01f)
    }
}
