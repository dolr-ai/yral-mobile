package com.yral.shared.libs.videoplayback

internal const val PLAYBACK_AFTER_RELEASE_REPORT_DWELL_MS = 2_000L

internal fun shouldReportPlaybackErrorAfterRelease(
    playStartMs: Long?,
    nowMs: Long,
    firstFramePending: Boolean,
    minDwellMs: Long = PLAYBACK_AFTER_RELEASE_REPORT_DWELL_MS,
): Boolean =
    playStartMs?.let { startMs ->
        firstFramePending && nowMs - startMs >= minDwellMs
    } ?: false
