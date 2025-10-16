package com.yral.shared.libs.designsystem.component

import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import co.touchlab.kermit.Logger
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.crashlytics.core.CrashlyticsManager

@Suppress("TooGenericExceptionCaught")
@Composable
internal actual fun SoundFeedback(
    soundUri: String,
    onPlayed: () -> Unit,
    crashlyticsManager: CrashlyticsManager,
) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        var mediaPlayer: MediaPlayer? = null
        Logger.d("SoundAndHaptics") { "Playing $soundUri" }
        try {
            val fileName = soundUri.removePrefix("file:///android_asset/")
            mediaPlayer = MediaPlayer()
            context.assets.openFd(fileName).use { afd ->
                mediaPlayer.setDataSource(afd)
            }
            mediaPlayer.prepare()
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener { onPlayed() }
        } catch (e: Exception) {
            crashlyticsManager.recordException(YralException("Error in dispatching sound $e"))
        }
        onDispose {
            try {
                mediaPlayer?.release()
            } catch (e: Exception) {
                crashlyticsManager.recordException(YralException("Error in releasing media player $e"))
            }
        }
    }
}
