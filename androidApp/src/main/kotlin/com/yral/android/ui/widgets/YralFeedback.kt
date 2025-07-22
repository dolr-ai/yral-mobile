package com.yral.android.ui.widgets

import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import co.touchlab.kermit.Logger

@Composable
fun YralFeedback(
    sound: Int,
    withHapticFeedback: Boolean = false,
    hapticFeedbackType: HapticFeedbackType = HapticFeedbackType.ContextClick,
    onPlayed: () -> Unit = {},
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(Unit) {
        Logger.d("SoundAndHaptics") { "Playing $sound" }
        if (withHapticFeedback) {
            haptic.performHapticFeedback(hapticFeedbackType)
        }
        val soundRes = sound
        val mediaPlayer = MediaPlayer.create(context, soundRes)
        mediaPlayer.start()
        mediaPlayer.setOnCompletionListener {
            it.release()
            onPlayed()
        }
    }
}
