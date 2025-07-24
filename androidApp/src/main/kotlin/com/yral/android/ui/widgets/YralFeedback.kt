package com.yral.android.ui.widgets

import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import co.touchlab.kermit.Logger
import kotlinx.coroutines.delay

private const val HAPTIC_FEEDBACK_DELAY = 30L

@Composable
fun YralFeedback(
    sound: Int,
    withHapticFeedback: Boolean = false,
    hapticFeedbackType: HapticFeedbackType = HapticFeedbackType.ContextClick,
    onPlayed: () -> Unit = {},
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    DisposableEffect(Unit) {
        Logger.d("SoundAndHaptics") { "Playing $sound" }
        val mediaPlayer = MediaPlayer.create(context, sound)
        mediaPlayer.start()
        mediaPlayer.setOnCompletionListener { onPlayed() }
        onDispose {
            mediaPlayer.release()
        }
    }
    LaunchedEffect(Unit) {
        Logger.d("SoundAndHaptics") { "Playing $hapticFeedbackType" }
        val hapticDuration = hapticFeedbackType.extendedHapticDuration()
        if (withHapticFeedback) {
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < hapticDuration) {
                haptic.performHapticFeedback(hapticFeedbackType)
                delay(HAPTIC_FEEDBACK_DELAY)
            }
        }
    }
}

@Suppress("MagicNumber")
private fun HapticFeedbackType.extendedHapticDuration(): Long =
    when (this) {
        HapticFeedbackType.LongPress -> 500L
        else -> HAPTIC_FEEDBACK_DELAY // only 1 loop
    }
