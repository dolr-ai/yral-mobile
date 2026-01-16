package com.shortform.video

data class PreloadScheduleResult(
    val window: PreloadWindow,
    val toStart: Set<Int>,
    val toCancel: Set<Int>,
)

class PreloadEventScheduler(
    private val policy: PreloadPolicy,
    private val reporter: PlaybackEventReporter,
) {
    private var scheduled: Set<Int> = emptySet()

    fun update(
        centerIndex: Int,
        itemCount: Int,
        idAt: (Int) -> String?,
    ): PreloadScheduleResult {
        val window = computePreloadWindow(centerIndex, itemCount, policy)
        val target = window.all
        val toStart = target - scheduled
        val toCancel = scheduled - target

        for (index in toStart) {
            val id = idAt(index) ?: continue
            val mode = if (index in window.prepared) "prepared" else "disk"
            reporter.preloadScheduled(id, index, index - centerIndex, mode)
        }

        for (index in toCancel) {
            val id = idAt(index) ?: continue
            reporter.preloadCanceled(id, index, "window_shift")
        }

        scheduled = target
        return PreloadScheduleResult(window, toStart, toCancel)
    }

    fun reset(reason: String, idAt: (Int) -> String?) {
        for (index in scheduled) {
            val id = idAt(index) ?: continue
            reporter.preloadCanceled(id, index, reason)
        }
        scheduled = emptySet()
    }
}
