package com.yral.shared.libs.videoplayback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlaybackProgress(
    val id: String,
    val index: Int,
    val positionMs: Long,
    val durationMs: Long,
)

class PlaybackProgressTicker(
    private val intervalMs: Long,
    private val scope: CoroutineScope,
    private val provider: () -> PlaybackProgress?,
    private val onProgress: (PlaybackProgress) -> Unit,
) {
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job =
            scope.launch {
                try {
                    while (isActive) {
                        try {
                            provider()?.let(onProgress)
                        } catch (_: Exception) {
                            // Keep ticking even if one iteration fails.
                        }
                        delay(intervalMs)
                    }
                } finally {
                    job = null
                }
            }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
