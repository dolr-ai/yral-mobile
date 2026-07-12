package com.yral.shared.features.chat.ui.conversation.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import co.touchlab.kermit.Logger
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
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
import platform.AVFAudio.setActive
import platform.Foundation.NSError
import platform.Foundation.NSURL

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
            configurePlaybackSession()
            val url = NSURL.fileURLWithPath(filePath)
            val audioPlayer = AVAudioPlayer(url, null)
            if (!audioPlayer.prepareToPlay()) {
                Logger.e { "AVAudioPlayer prepareToPlay failed for $filePath" }
                _state.value = AudioPlayerState.Idle
                return
            }
            player = audioPlayer
            loadedFilePath = filePath
            val durationMs = (audioPlayer.duration * MS_PER_SECOND).toLong()
            _state.value = AudioPlayerState.Ready(durationMs)
        }.onFailure { t ->
            Logger.e(t) { "Audio player load failed for $filePath" }
            _state.value = AudioPlayerState.Idle
        }
    }

    override fun playPause() {
        val audioPlayer = player ?: return
        when (val playerState = _state.value) {
            is AudioPlayerState.Ready, is AudioPlayerState.Paused -> {
                if (!audioPlayer.play()) {
                    Logger.w { "AVAudioPlayer play() returned false" }
                    return
                }
                _state.value =
                    AudioPlayerState.Playing(
                        positionMs = (audioPlayer.currentTime * MS_PER_SECOND).toLong(),
                        durationMs = (audioPlayer.duration * MS_PER_SECOND).toLong(),
                    )
                startTicker()
            }

            is AudioPlayerState.Playing -> {
                audioPlayer.pause()
                tickerJob?.cancel()
                tickerJob = null
                _state.value =
                    AudioPlayerState.Paused(
                        positionMs = (audioPlayer.currentTime * MS_PER_SECOND).toLong(),
                        durationMs = playerState.durationMs,
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
        tickerJob =
            scope.launch {
                while (isActive) {
                    delay(PLAYER_TICK_MS)
                    val audioPlayer = player ?: return@launch
                    if (!audioPlayer.playing) {
                        // Playback finished — collapse to Ready
                        val durationMs = (audioPlayer.duration * MS_PER_SECOND).toLong()
                        _state.value = AudioPlayerState.Ready(durationMs)
                        return@launch
                    }
                    _state.value =
                        AudioPlayerState.Playing(
                            positionMs = (audioPlayer.currentTime * MS_PER_SECOND).toLong(),
                            durationMs = (audioPlayer.duration * MS_PER_SECOND).toLong(),
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

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun configurePlaybackSession() {
        memScoped {
            val session = AVAudioSession.sharedInstance()
            val errorPtr = alloc<ObjCObjectVar<NSError?>>()

            errorPtr.value = null
            if (!session.setCategory(AVAudioSessionCategoryPlayback, error = errorPtr.ptr)) {
                logAudioSessionFailure("setCategory", errorPtr.value)
            }

            errorPtr.value = null
            if (!session.setActive(true, error = errorPtr.ptr)) {
                logAudioSessionFailure("setActive", errorPtr.value)
            }
        }
    }

    private fun logAudioSessionFailure(
        stage: String,
        error: NSError?,
    ) {
        val reason = error?.localizedDescription ?: "unknown error"
        Logger.w { "AVAudioSession $stage failed: $reason" }
    }
}
