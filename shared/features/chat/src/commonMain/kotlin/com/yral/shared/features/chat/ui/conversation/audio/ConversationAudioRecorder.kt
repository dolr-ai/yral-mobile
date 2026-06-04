@file:Suppress("MatchingDeclarationName", "ktlint:standard:filename")

package com.yral.shared.features.chat.ui.conversation.audio

import androidx.compose.runtime.Composable
import com.yral.shared.features.chat.attachments.FilePathChatAttachment
import kotlinx.coroutines.flow.StateFlow

/**
 * Voice-recording controller for the chat composer. Platform actuals own:
 *   - microphone permission flow (RECORD_AUDIO on Android, NSMicrophoneUsageDescription on iOS)
 *   - the OS recorder (MediaRecorder on Android, AVAudioRecorder on iOS)
 *   - a cache file the recording is written to
 *
 * Lifecycle: [start] → [Recording] → [stop] (fires onComplete) OR [cancel]
 * (discards file). [permissionDenied] fires when the user declined the
 * mic permission so the screen can show a "open settings" affordance.
 *
 * MIME / extension contract: the file passed to onComplete is .m4a /
 * audio/mp4 on Android (MediaRecorder.OutputFormat.MPEG_4 + AudioEncoder.AAC)
 * and .m4a / audio/mp4 on iOS (kAudioFormatMPEG4AAC). The backend
 * /api/v1/media/upload route accepts both via the audio extension allowlist
 * (storage.kt:14, .m4a → audio/mp4).
 */
interface AudioRecorderController {
    val state: StateFlow<AudioRecordingState>
    fun start()
    fun stop()
    fun cancel()
}

@Composable
expect fun rememberChatAudioRecorder(
    onComplete: (attachment: FilePathChatAttachment, durationSeconds: Int) -> Unit,
    onPermissionDenied: () -> Unit,
): AudioRecorderController
