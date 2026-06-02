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
            val p = MediaPlayer()
            p.setDataSource(filePath)
            p.prepare()
            p.setOnCompletionListener {
                tickerJob?.cancel()
                tickerJob = null
                val dur = p.duration.toLong()
                _state.value = AudioPlayerState.Ready(dur)
            }
            player = p
            loadedFilePath = filePath
            _state.value = AudioPlayerState.Ready(p.duration.toLong())
        }.onFailure { t ->
            Logger.e(t) { "Audio player load failed for $filePath" }
            _state.value = AudioPlayerState.Idle
        }
    }

    override fun playPause() {
        val p = player ?: return
        when (val s = _state.value) {
            is AudioPlayerState.Ready, is AudioPlayerState.Paused -> {
                p.start()
                _state.value = AudioPlayerState.Playing(
                    positionMs = p.currentPosition.toLong(),
                    durationMs = p.duration.toLong(),
                )
                startTicker()
            }
            is AudioPlayerState.Playing -> {
                p.pause()
                tickerJob?.cancel()
                tickerJob = null
                _state.value = AudioPlayerState.Paused(
                    positionMs = p.currentPosition.toLong(),
                    durationMs = s.durationMs,
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
        tickerJob = scope.launch {
            while (isActive) {
                delay(PLAYER_TICK_MS)
                val p = player ?: return@launch
                if (!p.isPlaying) return@launch
                _state.value = AudioPlayerState.Playing(
                    positionMs = p.currentPosition.toLong(),
                    durationMs = p.duration.toLong(),
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
