@file:Suppress("MagicNumber")

package com.yral.shared.libs.videoPlayer.util

import com.yral.shared.libs.videoPlayer.model.Platform

internal fun isLiveStream(url: String): Boolean = url.endsWith(".m3u8")

internal fun formatMinSec(value: Int): String {
    return if (value == 0) {
        "00:00"
    } else {
        // Convert value from milliseconds to total seconds
        val totalSeconds = value / 1000

        // Calculate hours, minutes, and seconds
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        fun Int.twoDigits(): String = this.toString().padStart(2, '0')

        // Format the output string
        return if (hours > 0) {
            "${hours.twoDigits()}:${minutes.twoDigits()}:${seconds.twoDigits()}"
        } else {
            "${minutes.twoDigits()}:${seconds.twoDigits()}"
        }
    }
}

internal fun formatInterval(value: Int): Int = value * 1000

internal fun formatYoutubeInterval(value: Int): Int = value * 1000

fun isHlsUrl(url: String): Boolean =
    url.endsWith(".m3u8") ||
        url.contains("application/vnd.apple.mpegurl")

fun isDesktop(): Boolean = isPlatform() == Platform.Desktop

expect fun isPlatform(): Platform

fun <T> List<T>.nextN(
    startIndex: Int,
    n: Int,
): List<T> =
    if (startIndex + 1 < size) {
        subList(startIndex + 1, minOf(startIndex + n, size))
    } else {
        emptyList()
    }
