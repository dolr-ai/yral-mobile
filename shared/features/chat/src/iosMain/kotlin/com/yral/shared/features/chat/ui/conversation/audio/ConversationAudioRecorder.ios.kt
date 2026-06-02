package com.yral.shared.features.chat.ui.conversation.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import co.touchlab.kermit.Logger
import com.yral.shared.features.chat.attachments.FilePathChatAttachment
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import platform.AVFAudio.AVAudioApplication
import platform.AVFAudio.AVAudioRecorder
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVEncoderAudioQualityKey
import platform.AVFAudio.AVFormatIDKey
import platform.AVFAudio.AVNumberOfChannelsKey
import platform.AVFAudio.AVSampleRateKey
import platform.CoreAudioTypes.kAudioFormatMPEG4AAC
import platform.Foundation.NSError
import platform.Foundation.NSNumber
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.fileURLWithPath
import platform.Foundation.NSFileManager
import platform.darwin.NSObject
import kotlin.experimental.ExperimentalNativeApi
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val AUDIO_SAMPLE_RATE_HZ = 44_100
private const val AUDIO_CHANNELS = 1
private const val RECORDING_TICK_MS = 100L
private const val MS_PER_SECOND = 1000
private const val AVAudioQualityHigh = 96   // platform.AVFAudio constant (matches Foundation)

@OptIn(ExperimentalForeignApi::class, ExperimentalTime::class, ExperimentalNativeApi::class)
@Composable
actual fun rememberChatAudioRecorder(
    onComplete: (attachment: FilePathChatAttachment, durationSeconds: Int) -> Unit,
    onPermissionDenied: () -> Unit,
): AudioRecorderController {
    val scope = rememberCoroutineScope()
    val controller = remember(scope, onComplete, onPermissionDenied) {
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
        // AVAudioApplication.requestRecordPermissionWithCompletionHandler() is the
        // modern API (iOS 17+). Fall back via try/catch is unnecessary since
        // the project minSdk targets a version that has it.
        AVAudioApplication.requestRecordPermissionWithCompletionHandler { granted ->
            if (granted) {
                actuallyStart()
            } else {
                onPermissionDenied()
            }
        }
    }

    private fun actuallyStart() {
        runCatching {
            val session = AVAudioSession.sharedInstance()
            session.setCategory(AVAudioSessionCategoryPlayAndRecord, null)
            session.setActive(true, null)

            val fileName = "chat_audio_${NSUUID().UUIDString()}.m4a"
            val path = NSTemporaryDirectory() + fileName
            val url = NSURL.fileURLWithPath(path)

            val settings: Map<Any?, *> = mapOf<Any?, Any?>(
                AVFormatIDKey to NSNumber(unsignedInt = kAudioFormatMPEG4AAC),
                AVSampleRateKey to NSNumber(int = AUDIO_SAMPLE_RATE_HZ),
                AVNumberOfChannelsKey to NSNumber(int = AUDIO_CHANNELS),
                AVEncoderAudioQualityKey to NSNumber(int = AVAudioQualityHigh),
            )

            val errPtr: NSObject? = null
            @Suppress("UNCHECKED_CAST")
            val r = AVAudioRecorder(url, settings as Map<Any?, *>, null)
            if (!r.prepareToRecord()) {
                Logger.e { "AVAudioRecorder prepareToRecord failed" }
                return
            }
            if (!r.record()) {
                Logger.e { "AVAudioRecorder record() returned false" }
                return
            }
            recorder = r
            outputUrl = url
            outputPath = path
            startTimeMs = Clock.System.now().toEpochMilliseconds()
            _state.value = AudioRecordingState.Recording(0)
            tickerJob = scope.launch {
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
        val r = recorder ?: return
        val path = outputPath ?: return
        _state.value = AudioRecordingState.Finalizing
        tickerJob?.cancel()
        tickerJob = null
        val durationMs = Clock.System.now().toEpochMilliseconds() - startTimeMs
        r.stop()
        recorder = null

        // Restore audio session to playback so other apps' audio resumes
        runCatching {
            AVAudioSession.sharedInstance().setCategory(AVAudioSessionCategoryPlayback, null)
        }

        val fileExists = NSFileManager.defaultManager.fileExistsAtPath(path)
        if (fileExists) {
            val attachment = FilePathChatAttachment(
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
}
