package com.yral.android.ui.widgets

import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import co.touchlab.kermit.Logger

@Composable
fun YralPlaySound(
    sound: Int,
    onPlayed: () -> Unit = {},
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        Logger.d("SoundAndHaptics") { "Playing $sound" }
        val soundRes = sound
        val mediaPlayer = MediaPlayer.create(context, soundRes)
        mediaPlayer.start()
        mediaPlayer.setOnCompletionListener {
            it.release()
            onPlayed()
        }
    }
}
