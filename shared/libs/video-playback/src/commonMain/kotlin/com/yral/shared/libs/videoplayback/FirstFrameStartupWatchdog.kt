package com.yral.shared.libs.videoplayback

internal enum class FirstFrameStartupAction {
    None,
    Resume,
    Rebuild,
    GiveUp,
}

internal data class FirstFrameStartupWatchdogConfig(
    // A gentle re-play nudge fires after this long if no first frame has rendered.
    val resumeTimeoutMs: Long = 1_500,
    // The destructive rebuild (replace item + restart from zero) only fires this long AFTER
    // the resume nudge, i.e. ~4s after startup. With the prefetch cache and stable
    // coordinator, a healthy video renders its first frame well under 2s, so anything still
    // blank at 4s is genuinely wedged and retrying sooner beats making the user wait.
    val rebuildTimeoutMs: Long = 2_500,
    val maxRebuildAttempts: Int = 1,
)

internal class FirstFrameStartupWatchdog(
    private val config: FirstFrameStartupWatchdogConfig = FirstFrameStartupWatchdogConfig(),
) {
    private var pendingIndex: Int? = null
    private var startMs: Long = 0
    private var lastActionMs: Long = 0
    private var resumeRequested = false
    private var rebuildAttempts = 0
    private var gaveUp = false

    fun start(
        index: Int,
        nowMs: Long,
    ) {
        pendingIndex = index
        startMs = nowMs
        lastActionMs = 0
        resumeRequested = false
        rebuildAttempts = 0
        gaveUp = false
    }

    fun clear(index: Int? = null) {
        if (index != null && pendingIndex != index) return
        pendingIndex = null
        startMs = 0
        lastActionMs = 0
        resumeRequested = false
        rebuildAttempts = 0
        gaveUp = false
    }

    @Suppress("ReturnCount")
    fun evaluate(
        index: Int,
        nowMs: Long,
        firstFramePending: Boolean,
    ): FirstFrameStartupAction {
        if (!firstFramePending || pendingIndex != index || gaveUp) return FirstFrameStartupAction.None

        val elapsedMs = nowMs - startMs
        if (!resumeRequested) {
            if (elapsedMs < config.resumeTimeoutMs) return FirstFrameStartupAction.None
            resumeRequested = true
            lastActionMs = nowMs
            return FirstFrameStartupAction.Resume
        }

        if (nowMs - lastActionMs < config.rebuildTimeoutMs) return FirstFrameStartupAction.None

        if (rebuildAttempts < config.maxRebuildAttempts) {
            rebuildAttempts++
            lastActionMs = nowMs
            return FirstFrameStartupAction.Rebuild
        }

        gaveUp = true
        return FirstFrameStartupAction.GiveUp
    }
}
