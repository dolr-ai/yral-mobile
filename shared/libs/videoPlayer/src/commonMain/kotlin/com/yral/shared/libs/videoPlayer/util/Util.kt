package com.yral.shared.libs.videoPlayer.util

fun isHlsUrl(url: String): Boolean =
    url.endsWith(".m3u8") ||
        url.contains("application/vnd.apple.mpegurl")

fun <T> List<T>.nextN(
    startIndex: Int,
    n: Int,
): List<T> =
    if (startIndex + 1 < size) {
        subList(startIndex + 1, minOf(startIndex + n, size))
    } else {
        emptyList()
    }
