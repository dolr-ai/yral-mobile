package com.yral.shared.features.chat.ui.conversation.audio

import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val PLAYER_TICK_MS = 100L

@Composable
actual fun rememberChatAudioPlayer(): AudioPlayerController {
    val scope = rememberCoroutineScope()
    val controller = remember(scope) { AndroidAudioPlayerController(scope) }
    DisposableEffect(controller) {
        onDispose { controller.stop() }
    }
    return controller
}

private class AndroidAudioPlayerController(
    private val scope: CoroutineScope,
) : AudioPlayerController {
    private val _state = MutableStateFlow<AudioPlayerState>(AudioPlayerState.Idle)
    override val state: StateFlow<AudioPlayerState> = _state.asStateFlow()

    private var player: MediaPlayer? = null
    private var loadedFilePath: String? = null
    private var tickerJob: Job? = null

    override fun load(filePath: String) {
        // Reuse the same MediaPlayer if the file path didn't change — avoids
        // reload latency on each play tap.
        if (loadedFilePath == filePath && player != null) return
        runCatching {
            releaseInternal()
            val mediaPlayer = MediaPlayer()
            mediaPlayer.setDataSource(filePath)
            mediaPlayer.prepare()
            mediaPlayer.setOnCompletionListener {
                tickerJob?.cancel()
                tickerJob = null
                val durationMs = mediaPlayer.duration.toLong()
                _state.value = AudioPlayerState.Ready(durationMs)
            }
            player = mediaPlayer
            loadedFilePath = filePath
            _state.value = AudioPlayerState.Ready(mediaPlayer.duration.toLong())
        }.onFailure { t ->
            Logger.e(t) { "Audio player load failed for $filePath" }
            _state.value = AudioPlayerState.Idle
        }
    }

    override fun playPause() {
        val mediaPlayer = player ?: return
        when (val playerState = _state.value) {
            is AudioPlayerState.Ready, is AudioPlayerState.Paused -> {
                mediaPlayer.start()
                _state.value =
                    AudioPlayerState.Playing(
                        positionMs = mediaPlayer.currentPosition.toLong(),
                        durationMs = mediaPlayer.duration.toLong(),
                    )
                startTicker()
            }

            is AudioPlayerState.Playing -> {
                mediaPlayer.pause()
                tickerJob?.cancel()
                tickerJob = null
                _state.value =
                    AudioPlayerState.Paused(
                        positionMs = mediaPlayer.currentPosition.toLong(),
                        durationMs = playerState.durationMs,
                    )
            }

            AudioPlayerState.Idle -> { /* not loaded yet */ }
        }
    }

    override fun stop() {
        releaseInternal()
        _state.value = AudioPlayerState.Idle
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob =
            scope.launch {
                while (isActive) {
                    delay(PLAYER_TICK_MS)
                    val mediaPlayer = player ?: return@launch
                    if (!mediaPlayer.isPlaying) return@launch
                    _state.value =
                        AudioPlayerState.Playing(
                            positionMs = mediaPlayer.currentPosition.toLong(),
                            durationMs = mediaPlayer.duration.toLong(),
                        )
                }
            }
    }

    private fun releaseInternal() {
        tickerJob?.cancel()
        tickerJob = null
        player?.let { runCatching { it.release() } }
        player = null
        loadedFilePath = null
    }
}
