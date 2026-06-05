package com.yral.shared.features.chat.ui.conversation.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import co.touchlab.kermit.Logger
import com.yral.shared.features.chat.attachments.FilePathChatAttachment
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
import platform.AVFAudio.AVAudioRecorder
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVEncoderAudioQualityKey
import platform.AVFAudio.AVFormatIDKey
import platform.AVFAudio.AVNumberOfChannelsKey
import platform.AVFAudio.AVSampleRateKey
import platform.AVFAudio.setActive
import platform.CoreAudioTypes.kAudioFormatMPEG4AAC
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSNumber
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import kotlin.experimental.ExperimentalNativeApi
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val AUDIO_SAMPLE_RATE_HZ = 44_100
private const val AUDIO_CHANNELS = 1
private const val RECORDING_TICK_MS = 100L
private const val MS_PER_SECOND = 1000
private const val AV_AUDIO_QUALITY_HIGH = 96

@OptIn(ExperimentalForeignApi::class, ExperimentalTime::class, ExperimentalNativeApi::class)
@Composable
actual fun rememberChatAudioRecorder(
    onComplete: (attachment: FilePathChatAttachment, durationSeconds: Int) -> Unit,
    onPermissionDenied: () -> Unit,
): AudioRecorderController {
    val scope = rememberCoroutineScope()
    val controller =
        remember(scope, onComplete, onPermissionDenied) {
            IosAudioRecorderController(scope, onComplete, onPermissionDenied)
        }
    DisposableEffect(controller) {
        onDispose { controller.releaseInternal() }
    }
    return controller
}

@OptIn(ExperimentalForeignApi::class, ExperimentalTime::class, ExperimentalNativeApi::class)
private class IosAudioRecorderController(
    private val scope: CoroutineScope,
    private val onComplete: (FilePathChatAttachment, Int) -> Unit,
    private val onPermissionDenied: () -> Unit,
) : AudioRecorderController {
    private val _state = MutableStateFlow<AudioRecordingState>(AudioRecordingState.Idle)
    override val state: StateFlow<AudioRecordingState> = _state.asStateFlow()

    private var recorder: AVAudioRecorder? = null
    private var outputUrl: NSURL? = null
    private var outputPath: String? = null
    private var startTimeMs: Long = 0
    private var tickerJob: Job? = null

    override fun start() {
        if (_state.value != AudioRecordingState.Idle) return
        AVAudioSession.sharedInstance().requestRecordPermission { granted ->
            if (granted) {
                actuallyStart()
            } else {
                onPermissionDenied()
            }
        }
    }

    private fun actuallyStart() {
        runCatching {
            configureRecordingSession()

            val fileName = "chat_audio_${NSUUID().UUIDString()}.m4a"
            val path = NSTemporaryDirectory() + fileName
            val url = NSURL.fileURLWithPath(path)

            val settings: Map<Any?, *> =
                mapOf<Any?, Any?>(
                    AVFormatIDKey to NSNumber(unsignedInt = kAudioFormatMPEG4AAC),
                    AVSampleRateKey to NSNumber(int = AUDIO_SAMPLE_RATE_HZ),
                    AVNumberOfChannelsKey to NSNumber(int = AUDIO_CHANNELS),
                    AVEncoderAudioQualityKey to NSNumber(int = AV_AUDIO_QUALITY_HIGH),
                )

            val audioRecorder = AVAudioRecorder(url, settings, null)
            if (!audioRecorder.prepareToRecord()) {
                Logger.e { "AVAudioRecorder prepareToRecord failed" }
                cleanup()
                return
            }
            if (!audioRecorder.record()) {
                Logger.e { "AVAudioRecorder record() returned false" }
                cleanup()
                return
            }
            recorder = audioRecorder
            outputUrl = url
            outputPath = path
            startTimeMs = Clock.System.now().toEpochMilliseconds()
            _state.value = AudioRecordingState.Recording(0)
            tickerJob =
                scope.launch {
                    while (isActive) {
                        delay(RECORDING_TICK_MS)
                        val elapsed = Clock.System.now().toEpochMilliseconds() - startTimeMs
                        val current = _state.value
                        if (current is AudioRecordingState.Recording) {
                            _state.value = AudioRecordingState.Recording(elapsed)
                        } else {
                            return@launch
                        }
                    }
                }
        }.onFailure { t ->
            Logger.e(t) { "AVAudioRecorder start failed" }
            cleanup()
            _state.value = AudioRecordingState.Idle
        }
    }

    override fun stop() {
        val audioRecorder = recorder ?: return
        val path = outputPath ?: return
        _state.value = AudioRecordingState.Finalizing
        tickerJob?.cancel()
        tickerJob = null
        val durationMs = Clock.System.now().toEpochMilliseconds() - startTimeMs
        audioRecorder.stop()
        recorder = null

        // Restore audio session to playback so other apps' audio resumes
        configurePlaybackSession()

        val fileExists = NSFileManager.defaultManager.fileExistsAtPath(path)
        if (fileExists) {
            val attachment =
                FilePathChatAttachment(
                    filePath = path,
                    fileName = path.substringAfterLast("/"),
                    contentType = "audio/mp4",
                )
            val durationSeconds = (durationMs / MS_PER_SECOND).toInt().coerceAtLeast(1)
            onComplete(attachment, durationSeconds)
        } else {
            Logger.w { "AVAudioRecorder produced no file at $path — discarded" }
        }
        outputUrl = null
        outputPath = null
        _state.value = AudioRecordingState.Idle
    }

    override fun cancel() {
        tickerJob?.cancel()
        tickerJob = null
        recorder?.stop()
        recorder = null
        outputPath?.let { path ->
            runCatching {
                NSFileManager.defaultManager.removeItemAtPath(path, error = null)
            }
        }
        outputUrl = null
        outputPath = null
        _state.value = AudioRecordingState.Idle
    }

    fun releaseInternal() {
        tickerJob?.cancel()
        tickerJob = null
        recorder?.stop()
        recorder = null
        // outputPath retained — preview composable may still own it; cleanup on its cancel.
        outputUrl = null
        outputPath = null
    }

    private fun cleanup() {
        tickerJob?.cancel()
        tickerJob = null
        recorder?.stop()
        recorder = null
        outputPath?.let { path ->
            runCatching {
                NSFileManager.defaultManager.removeItemAtPath(path, error = null)
            }
        }
        outputUrl = null
        outputPath = null
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun configureRecordingSession() {
        memScoped {
            val session = AVAudioSession.sharedInstance()
            val errorPtr = alloc<ObjCObjectVar<NSError?>>()

            errorPtr.value = null
            if (!session.setCategory(AVAudioSessionCategoryPlayAndRecord, error = errorPtr.ptr)) {
                logAudioSessionFailure("setCategory(playAndRecord)", errorPtr.value)
            }

            errorPtr.value = null
            if (!session.setActive(true, error = errorPtr.ptr)) {
                logAudioSessionFailure("setActive", errorPtr.value)
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun configurePlaybackSession() {
        memScoped {
            val session = AVAudioSession.sharedInstance()
            val errorPtr = alloc<ObjCObjectVar<NSError?>>()

            errorPtr.value = null
            if (!session.setCategory(AVAudioSessionCategoryPlayback, error = errorPtr.ptr)) {
                logAudioSessionFailure("setCategory(playback)", errorPtr.value)
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
