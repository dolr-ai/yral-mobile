package com.yral.shared.libs.videoplayback

internal enum class FirstFrameStartupAction {
    None,
    Resume,
    Rebuild,
    GiveUp,
}

internal data class FirstFrameStartupWatchdogConfig(
    val resumeTimeoutMs: Long = 1_500,
    val rebuildTimeoutMs: Long = 3_000,
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
