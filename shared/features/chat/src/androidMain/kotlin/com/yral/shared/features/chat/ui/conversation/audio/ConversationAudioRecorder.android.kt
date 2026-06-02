package com.yral.shared.features.chat.ui.conversation.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import co.touchlab.kermit.Logger
import com.yral.shared.features.chat.attachments.FilePathChatAttachment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val AUDIO_SAMPLE_RATE_HZ = 44_100
private const val AUDIO_BIT_RATE = 128_000
private const val RECORDING_TICK_MS = 100L
private const val MS_PER_SECOND = 1000

@OptIn(ExperimentalTime::class)
@Composable
actual fun rememberChatAudioRecorder(
    onComplete: (attachment: FilePathChatAttachment, durationSeconds: Int) -> Unit,
    onPermissionDenied: () -> Unit,
): AudioRecorderController {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val controller =
        remember(context, scope, onComplete, onPermissionDenied) {
            AndroidAudioRecorderController(context, scope, onComplete, onPermissionDenied)
        }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { granted ->
            if (granted) {
                controller.actuallyStart()
            } else {
                onPermissionDenied()
            }
        }

    DisposableEffect(controller) {
        controller.permissionRequest = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
        onDispose { controller.releaseInternal() }
    }

    return controller
}

@OptIn(ExperimentalTime::class)
private class AndroidAudioRecorderController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onComplete: (FilePathChatAttachment, Int) -> Unit,
    private val onPermissionDenied: () -> Unit,
) : AudioRecorderController {
    private val _state = MutableStateFlow<AudioRecordingState>(AudioRecordingState.Idle)
    override val state: StateFlow<AudioRecordingState> = _state.asStateFlow()

    var permissionRequest: (() -> Unit)? = null

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startTimeMs: Long = 0
    private var tickerJob: Job? = null

    override fun start() {
        if (_state.value != AudioRecordingState.Idle) return
        val granted =
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        if (granted) {
            actuallyStart()
        } else {
            permissionRequest?.invoke() ?: onPermissionDenied()
        }
    }

    fun actuallyStart() {
        runCatching {
            val file = File.createTempFile(
                "chat_audio_${Clock.System.now().toEpochMilliseconds()}",
                ".m4a",
                context.cacheDir,
            )
            outputFile = file

            @Suppress("DEPRECATION")
            val r =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(context)
                } else {
                    MediaRecorder()
                }
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioSamplingRate(AUDIO_SAMPLE_RATE_HZ)
            r.setAudioEncodingBitRate(AUDIO_BIT_RATE)
            r.setOutputFile(file.absolutePath)
            r.prepare()
            r.start()
            recorder = r
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
            Logger.e(t) { "Audio recorder start failed" }
            cleanup()
            _state.value = AudioRecordingState.Idle
        }
    }

    override fun stop() {
        val r = recorder ?: return
        val file = outputFile ?: return
        _state.value = AudioRecordingState.Finalizing
        tickerJob?.cancel()
        tickerJob = null
        val durationMs = Clock.System.now().toEpochMilliseconds() - startTimeMs

        runCatching {
            r.stop()
            r.release()
        }.onFailure { t -> Logger.w(t) { "Audio recorder stop failed (file may be empty)" } }
        recorder = null

        if (file.exists() && file.length() > 0) {
            val attachment =
                FilePathChatAttachment(
                    filePath = file.absolutePath,
                    fileName = file.name,
                    contentType = "audio/mp4",
                )
            val durationSeconds = (durationMs / MS_PER_SECOND).toInt().coerceAtLeast(1)
            onComplete(attachment, durationSeconds)
        } else {
            // Recorder produced a zero-byte file (typically user tapped stop within
            // the first ~100ms before MediaRecorder accumulated a writable frame).
            // Discard; the screen will treat this as a no-op and stay on the
            // ChatInput state.
            file.takeIf { it.exists() }?.delete()
            Logger.w { "Audio recording produced empty file — discarded" }
        }
        outputFile = null
        _state.value = AudioRecordingState.Idle
    }

    override fun cancel() {
        tickerJob?.cancel()
        tickerJob = null
        recorder?.let { r ->
            runCatching {
                r.stop()
            }.onFailure { /* swallow — stop before writable frame is a known MediaRecorder gotcha */ }
            runCatching { r.release() }
        }
        recorder = null
        outputFile?.takeIf { it.exists() }?.delete()
        outputFile = null
        _state.value = AudioRecordingState.Idle
    }

    fun releaseInternal() {
        tickerJob?.cancel()
        tickerJob = null
        recorder?.let { runCatching { it.release() } }
        recorder = null
        // Don't delete outputFile here — the caller (preview composable) may still
        // own a reference until the user picks delete or send. The composable lifecycle
        // handles cleanup via FilePathChatAttachment.deleteCachedFile() on cancel.
        outputFile = null
    }

    private fun cleanup() {
        tickerJob?.cancel()
        tickerJob = null
        recorder?.let { runCatching { it.release() } }
        recorder = null
        outputFile?.takeIf { it.exists() }?.delete()
        outputFile = null
    }
}
