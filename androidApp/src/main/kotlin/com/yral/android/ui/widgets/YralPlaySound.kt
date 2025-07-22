package com.yral.android.ui.widgets

import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

@Composable
fun YralPlaySound(
    shouldPlay: Boolean = true,
    sound: Int,
) {
    val context = LocalContext.current
    LaunchedEffect(shouldPlay) {
        if (shouldPlay) {
            val soundRes = sound
            val mediaPlayer = MediaPlayer.create(context, soundRes)
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener {
                it.release()
            }
        }
    }
}
