package com.yral.shared.libs.videoplayback

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AfterReleasePlaybackErrorGateTest {
    @Test
    fun `does not report before dwell threshold`() {
        assertFalse(
            shouldReportPlaybackErrorAfterRelease(
                playStartMs = 1_000,
                nowMs = 2_999,
                firstFramePending = true,
            ),
        )
    }

    @Test
    fun `reports at dwell threshold`() {
        assertTrue(
            shouldReportPlaybackErrorAfterRelease(
                playStartMs = 1_000,
                nowMs = 3_000,
                firstFramePending = true,
            ),
        )
    }

    @Test
    fun `reports after dwell threshold`() {
        assertTrue(
            shouldReportPlaybackErrorAfterRelease(
                playStartMs = 1_000,
                nowMs = 4_500,
                firstFramePending = true,
            ),
        )
    }

    @Test
    fun `does not report when first frame is not pending`() {
        assertFalse(
            shouldReportPlaybackErrorAfterRelease(
                playStartMs = 1_000,
                nowMs = 4_500,
                firstFramePending = false,
            ),
        )
    }

    @Test
    fun `does not report without play start time`() {
        assertFalse(
            shouldReportPlaybackErrorAfterRelease(
                playStartMs = null,
                nowMs = 4_500,
                firstFramePending = true,
            ),
        )
    }

    @Test
    fun `does not report when elapsed time is negative`() {
        assertFalse(
            shouldReportPlaybackErrorAfterRelease(
                playStartMs = 4_500,
                nowMs = 1_000,
                firstFramePending = true,
            ),
        )
    }
}
