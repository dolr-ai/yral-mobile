package com.yral.shared.features.chat.ui.conversation.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import co.touchlab.kermit.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.Foundation.NSURL
import platform.Foundation.fileURLWithPath

private const val PLAYER_TICK_MS = 100L
private const val MS_PER_SECOND = 1000

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberChatAudioPlayer(): AudioPlayerController {
    val scope = rememberCoroutineScope()
    val controller = remember(scope) { IosAudioPlayerController(scope) }
    DisposableEffect(controller) {
        onDispose { controller.stop() }
    }
    return controller
}

@OptIn(ExperimentalForeignApi::class)
private class IosAudioPlayerController(
    private val scope: CoroutineScope,
) : AudioPlayerController {
    private val _state = MutableStateFlow<AudioPlayerState>(AudioPlayerState.Idle)
    override val state: StateFlow<AudioPlayerState> = _state.asStateFlow()

    private var player: AVAudioPlayer? = null
    private var loadedFilePath: String? = null
    private var tickerJob: Job? = null

    override fun load(filePath: String) {
        if (loadedFilePath == filePath && player != null) return
        runCatching {
            releaseInternal()
            AVAudioSession.sharedInstance().setCategory(AVAudioSessionCategoryPlayback, null)
            AVAudioSession.sharedInstance().setActive(true, null)
            val url = NSURL.fileURLWithPath(filePath)
            val p = AVAudioPlayer(url, null)
            if (!p.prepareToPlay()) {
                Logger.e { "AVAudioPlayer prepareToPlay failed for $filePath" }
                _state.value = AudioPlayerState.Idle
                return
            }
            player = p
            loadedFilePath = filePath
            val durationMs = (p.duration * MS_PER_SECOND).toLong()
            _state.value = AudioPlayerState.Ready(durationMs)
        }.onFailure { t ->
            Logger.e(t) { "Audio player load failed for $filePath" }
            _state.value = AudioPlayerState.Idle
        }
    }

    override fun playPause() {
        val p = player ?: return
        when (val s = _state.value) {
            is AudioPlayerState.Ready, is AudioPlayerState.Paused -> {
                if (!p.play()) {
                    Logger.w { "AVAudioPlayer play() returned false" }
                    return
                }
                _state.value = AudioPlayerState.Playing(
                    positionMs = (p.currentTime * MS_PER_SECOND).toLong(),
                    durationMs = (p.duration * MS_PER_SECOND).toLong(),
                )
                startTicker()
            }
            is AudioPlayerState.Playing -> {
                p.pause()
                tickerJob?.cancel()
                tickerJob = null
                _state.value = AudioPlayerState.Paused(
                    positionMs = (p.currentTime * MS_PER_SECOND).toLong(),
                    durationMs = s.durationMs,
                )
            }
            AudioPlayerState.Idle -> { /* not loaded */ }
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
                if (!p.playing) {
                    // Playback finished — collapse to Ready
                    val dur = (p.duration * MS_PER_SECOND).toLong()
                    _state.value = AudioPlayerState.Ready(dur)
                    return@launch
                }
                _state.value = AudioPlayerState.Playing(
                    positionMs = (p.currentTime * MS_PER_SECOND).toLong(),
                    durationMs = (p.duration * MS_PER_SECOND).toLong(),
                )
            }
        }
    }

    private fun releaseInternal() {
        tickerJob?.cancel()
        tickerJob = null
        player?.stop()
        player = null
        loadedFilePath = null
    }
}
