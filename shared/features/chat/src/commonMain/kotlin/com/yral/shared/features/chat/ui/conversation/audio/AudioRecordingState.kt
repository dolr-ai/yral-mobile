package com.yral.shared.features.chat.ui.conversation.audio

/**
 * Recorder lifecycle for the in-chat voice-note feature.
 *
 *   Idle       — no recording in progress
 *   Recording  — actively capturing audio; [elapsedMs] is the live counter
 *   Finalizing — stop requested; waiting on the platform to flush the file
 *
 * The composable consumes [Recording.elapsedMs] for the live timer; the
 * platform recorder ticks it roughly every 100ms. On stop the recorder
 * fires the onComplete callback with the recorded file's [FilePathChatAttachment]
 * + duration-in-seconds, and resets to Idle.
 */
sealed class AudioRecordingState {
    object Idle : AudioRecordingState()
    data class Recording(val elapsedMs: Long) : AudioRecordingState()
    object Finalizing : AudioRecordingState()
}

/**
 * Player lifecycle for the recorded-audio preview composable.
 *
 *   Idle    — no file loaded
 *   Ready   — file loaded, not playing yet
 *   Playing — playback in progress; [positionMs] / [durationMs] for the scrubber
 *   Paused  — paused mid-playback, can resume
 *
 * Completion (playback reached end) collapses back to Ready so the user can
 * tap play again.
 */
sealed class AudioPlayerState {
    object Idle : AudioPlayerState()
    data class Ready(val durationMs: Long) : AudioPlayerState()
    data class Playing(val positionMs: Long, val durationMs: Long) : AudioPlayerState()
    data class Paused(val positionMs: Long, val durationMs: Long) : AudioPlayerState()
}
