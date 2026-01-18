package com.yral.shared.libs.videoplayback

class PreparedSlotScheduler(
    private val policy: PreloadPolicy,
    private val reporter: PlaybackEventReporter,
) {
    private var pendingIndex: Int? = null
    private var startMs: Long? = null
    private var prerollRequested = false

    @Suppress("ReturnCount")
    fun schedule(
        activeIndex: Int,
        itemCount: Int,
        idAt: (Int) -> String?,
        prepare: (Int) -> Unit,
    ) {
        if (!policy.usePreparedNextPlayer) return
        val nextIndex = activeIndex + 1
        if (nextIndex !in 0 until itemCount) return
        if (pendingIndex == nextIndex) return

        val id = idAt(nextIndex) ?: return
        prepare(nextIndex)
        pendingIndex = nextIndex
        startMs = null
        prerollRequested = false
        reporter.preloadScheduled(id, nextIndex, 1, "prepared")
    }

    @Suppress("ReturnCount")
    fun markReady(
        index: Int,
        nowMs: Long,
        idAt: (Int) -> String?,
        onPreroll: (() -> Unit)? = null,
    ) {
        if (!policy.usePreparedNextPlayer) return
        if (pendingIndex != index) return
        val id =
            idAt(index) ?: run {
                pendingIndex = null
                startMs = null
                prerollRequested = false
                return
            }
        if (!prerollRequested) {
            prerollRequested = true
            onPreroll?.invoke()
        }
        val started = startMs ?: nowMs
        reporter.preloadCompleted(id, index, 0, nowMs - started, false)
        pendingIndex = null
        startMs = null
    }

    fun markError(
        index: Int,
        idAt: (Int) -> String?,
        reason: String,
    ) {
        if (pendingIndex != index) return
        val id = idAt(index) ?: return
        reporter.preloadCanceled(id, index, reason)
        pendingIndex = null
        startMs = null
    }

    fun reset(
        reason: String,
        idAt: (Int) -> String?,
    ) {
        val index = pendingIndex ?: return
        val id = idAt(index) ?: return
        reporter.preloadCanceled(id, index, reason)
        pendingIndex = null
        startMs = null
    }

    fun clearOnSwap() {
        pendingIndex = null
        startMs = null
        prerollRequested = false
    }

    fun setStartTime(nowMs: Long) {
        startMs = nowMs
    }
}
