package com.yral.shared.libs.videoplayback

import kotlin.test.Test
import kotlin.test.assertEquals

class FirstFrameStartupWatchdogTest {
    private val config =
        FirstFrameStartupWatchdogConfig(
            resumeTimeoutMs = 100,
            rebuildTimeoutMs = 200,
            maxRebuildAttempts = 1,
        )

    @Test
    fun `does nothing before resume timeout`() {
        val watchdog = FirstFrameStartupWatchdog(config)
        watchdog.start(index = 2, nowMs = 0)

        assertEquals(
            FirstFrameStartupAction.None,
            watchdog.evaluate(index = 2, nowMs = 99, firstFramePending = true),
        )
    }

    @Test
    fun `requests resume at resume timeout`() {
        val watchdog = FirstFrameStartupWatchdog(config)
        watchdog.start(index = 2, nowMs = 0)

        assertEquals(
            FirstFrameStartupAction.Resume,
            watchdog.evaluate(index = 2, nowMs = 100, firstFramePending = true),
        )
    }

    @Test
    fun `requests rebuild when resume does not produce first frame`() {
        val watchdog = FirstFrameStartupWatchdog(config)
        watchdog.start(index = 2, nowMs = 0)
        watchdog.evaluate(index = 2, nowMs = 100, firstFramePending = true)

        assertEquals(
            FirstFrameStartupAction.Rebuild,
            watchdog.evaluate(index = 2, nowMs = 300, firstFramePending = true),
        )
    }

    @Test
    fun `gives up after rebuild retry cap`() {
        val watchdog = FirstFrameStartupWatchdog(config)
        watchdog.start(index = 2, nowMs = 0)
        watchdog.evaluate(index = 2, nowMs = 100, firstFramePending = true)
        watchdog.evaluate(index = 2, nowMs = 300, firstFramePending = true)

        assertEquals(
            FirstFrameStartupAction.GiveUp,
            watchdog.evaluate(index = 2, nowMs = 500, firstFramePending = true),
        )
        assertEquals(
            FirstFrameStartupAction.None,
            watchdog.evaluate(index = 2, nowMs = 700, firstFramePending = true),
        )
    }

    @Test
    fun `defers rebuild once while media is buffering`() {
        val watchdog = FirstFrameStartupWatchdog(config)
        watchdog.start(index = 2, nowMs = 0)
        watchdog.evaluate(index = 2, nowMs = 100, firstFramePending = true)

        // Rebuild is due, but data is arriving: hold off one window.
        assertEquals(
            FirstFrameStartupAction.None,
            watchdog.evaluate(index = 2, nowMs = 300, firstFramePending = true, hasBufferedMedia = true),
        )
        // Still no first frame a full window later: rebuild even though media is buffered.
        assertEquals(
            FirstFrameStartupAction.Rebuild,
            watchdog.evaluate(index = 2, nowMs = 500, firstFramePending = true, hasBufferedMedia = true),
        )
    }

    @Test
    fun `buffered media does not delay rebuild before its own timeout`() {
        val watchdog = FirstFrameStartupWatchdog(config)
        watchdog.start(index = 2, nowMs = 0)
        watchdog.evaluate(index = 2, nowMs = 100, firstFramePending = true)

        // The deferral only applies when a rebuild is actually due.
        assertEquals(
            FirstFrameStartupAction.None,
            watchdog.evaluate(index = 2, nowMs = 150, firstFramePending = true, hasBufferedMedia = true),
        )
        assertEquals(
            FirstFrameStartupAction.None,
            watchdog.evaluate(index = 2, nowMs = 300, firstFramePending = true, hasBufferedMedia = true),
        )
        assertEquals(
            FirstFrameStartupAction.Rebuild,
            watchdog.evaluate(index = 2, nowMs = 500, firstFramePending = true, hasBufferedMedia = false),
        )
    }

    @Test
    fun `clear prevents stale action`() {
        val watchdog = FirstFrameStartupWatchdog(config)
        watchdog.start(index = 2, nowMs = 0)
        watchdog.clear(index = 2)

        assertEquals(
            FirstFrameStartupAction.None,
            watchdog.evaluate(index = 2, nowMs = 500, firstFramePending = true),
        )
    }

    @Test
    fun `ignores non pending first frame`() {
        val watchdog = FirstFrameStartupWatchdog(config)
        watchdog.start(index = 2, nowMs = 0)

        assertEquals(
            FirstFrameStartupAction.None,
            watchdog.evaluate(index = 2, nowMs = 500, firstFramePending = false),
        )
    }
}
