@file:Suppress("MagicNumber")

package com.yral.shared.libs.videoPlayer.util

import android.annotation.SuppressLint
import com.yral.shared.libs.videoPlayer.model.Platform
import java.util.concurrent.TimeUnit

@SuppressLint("DefaultLocale")
@Suppress("ImplicitDefaultLocale")
internal actual fun formatMinSec(value: Int): String {
    return if (value == 0) {
        "00:00"
    } else {
        // Convert value from milliseconds to total seconds
        val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(value.toLong())

        // Calculate hours, minutes, and seconds
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        // Format the output string
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}

internal actual fun formatInterval(value: Int): Int = value * 1000

actual fun isPlatform(): Platform = Platform.Android

internal actual fun formatYoutubeInterval(value: Int): Int = value * 1000
