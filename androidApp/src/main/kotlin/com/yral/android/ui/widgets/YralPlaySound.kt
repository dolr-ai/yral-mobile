package com.yral.android.ui.widgets

import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import co.touchlab.kermit.Logger

@Composable
fun YralPlaySound(
    shouldPlay: Boolean = true,
    sound: Int,
    onSoundComplete: () -> Unit = {},
) {
    Logger.d("SoundAndHaptics") { "should play: $shouldPlay, sound: $sound" }
    val context = LocalContext.current
    LaunchedEffect(shouldPlay) {
        if (shouldPlay) {
            Logger.d("SoundAndHaptics") { "Playing $sound" }
            val soundRes = sound
            val mediaPlayer = MediaPlayer.create(context, soundRes)
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener {
                it.release()
                onSoundComplete()
            }
        }
    }
}
