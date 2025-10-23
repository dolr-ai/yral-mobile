package com.yral.shared.libs.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import co.touchlab.kermit.Logger
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.crashlytics.core.CrashlyticsManager
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioPlayerDelegateProtocol
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.darwin.NSObject

@Composable
internal actual fun SoundFeedback(
    soundUri: String,
    onPlayed: () -> Unit,
    crashlyticsManager: CrashlyticsManager,
) {
    val onPlayedState = rememberUpdatedState(onPlayed)
    val delegate =
        remember(crashlyticsManager) {
            SoundFeedbackDelegate(
                crashlyticsManager = crashlyticsManager,
            )
        }
    DisposableEffect(soundUri) {
        delegate.onPlayed = { onPlayedState.value.invoke() }
        Logger.d("SoundAndHaptics") { "Playing $soundUri" }
        val player =
            createAudioPlayer(
                soundUri = soundUri,
                crashlyticsManager = crashlyticsManager,
            )
        if (player == null) {
            onPlayed()
            onDispose {
                delegate.onPlayed = null
            }
        } else {
            player.delegate = delegate
            val started =
                runCatching { player.play() }
                    .onFailure {
                        crashlyticsManager.recordException(
                            YralException("Error starting audio playback $it"),
                        )
                    }.getOrDefault(false)
            if (!started) {
                crashlyticsManager.recordException(
                    YralException("AVAudioPlayer.play returned false for sound id $soundUri"),
                )
                onPlayed()
            }
            onDispose {
                runCatching { player.stop() }
                player.delegate = null
                delegate.onPlayed = null
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun createAudioPlayer(
    soundUri: String,
    crashlyticsManager: CrashlyticsManager,
): AVAudioPlayer? {
    val url = NSURL.fileURLWithPath(soundUri.removePrefix("file://"))
    return memScoped {
        val error = alloc<ObjCObjectVar<NSError?>>()
        val player = AVAudioPlayer(contentsOfURL = url, error = error.ptr)
        val nsError = error.value
        if (nsError != null) {
            crashlyticsManager.recordException(
                YralException("Error creating AVAudioPlayer ${nsError.localizedDescription}"),
            )
            null
        } else {
            player.prepareToPlay()
            player
        }
    }
}

private class SoundFeedbackDelegate(
    private val crashlyticsManager: CrashlyticsManager,
) : NSObject(),
    AVAudioPlayerDelegateProtocol {
    var onPlayed: (() -> Unit)? = null

    override fun audioPlayerDidFinishPlaying(
        player: AVAudioPlayer,
        successfully: Boolean,
    ) {
        onPlayed?.invoke()
    }

    override fun audioPlayerDecodeErrorDidOccur(
        player: AVAudioPlayer,
        error: NSError?,
    ) {
        crashlyticsManager.recordException(
            YralException("Error decoding audio ${error?.localizedDescription ?: "unknown"}"),
        )
        onPlayed?.invoke()
    }
}
