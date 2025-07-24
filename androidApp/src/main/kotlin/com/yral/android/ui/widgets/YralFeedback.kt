package com.yral.android.ui.widgets

import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import co.touchlab.kermit.Logger
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.crashlytics.core.CrashlyticsManager
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

private const val HAPTIC_FEEDBACK_DELAY = 30L

@Suppress("TooGenericExceptionCaught")
@Composable
fun YralFeedback(
    sound: Int,
    withHapticFeedback: Boolean = false,
    hapticFeedbackType: HapticFeedbackType = HapticFeedbackType.ContextClick,
    onPlayed: () -> Unit = {},
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val crashlyticsManager = koinInject<CrashlyticsManager>()
    DisposableEffect(Unit) {
        var mediaPlayer: MediaPlayer? = null
        Logger.d("SoundAndHaptics") { "Playing $sound" }
        try {
            mediaPlayer = MediaPlayer.create(context, sound)
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
    LaunchedEffect(Unit) {
        Logger.d("SoundAndHaptics") { "Playing $hapticFeedbackType" }
        try {
            val hapticDuration = hapticFeedbackType.extendedHapticDuration()
            if (withHapticFeedback) {
                val startTime = System.currentTimeMillis()
                while (System.currentTimeMillis() - startTime < hapticDuration) {
                    haptic.performHapticFeedback(hapticFeedbackType)
                    delay(HAPTIC_FEEDBACK_DELAY)
                }
            }
        } catch (e: Exception) {
            crashlyticsManager.recordException(YralException("Error in dispatching haptics $e"))
        }
    }
}

@Suppress("MagicNumber")
private fun HapticFeedbackType.extendedHapticDuration(): Long =
    when (this) {
        HapticFeedbackType.LongPress -> 500L
        else -> HAPTIC_FEEDBACK_DELAY // only 1 loop
    }
