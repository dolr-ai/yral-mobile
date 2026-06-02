package com.yral.shared.features.chat.ui.conversation.audio

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.StateFlow

/**
 * Playback controller for the recorded-audio preview composable.
 *
 * Loads a file path (the .m4a the recorder just wrote), plays / pauses,
 * exposes position + duration via state for the UI scrubber + timer.
 * stop() releases the underlying OS player so the file can be safely
 * deleted by the recorder if the user picks the discard affordance.
 */
interface AudioPlayerController {
    val state: StateFlow<AudioPlayerState>
    fun load(filePath: String)
    fun playPause()
    fun stop()
}

@Composable
expect fun rememberChatAudioPlayer(): AudioPlayerController
