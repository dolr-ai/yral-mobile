package com.yral.shared.libs.videoPlayer.util

import com.yral.shared.libs.videoPlayer.model.Platform

internal fun isLiveStream(url: String): Boolean = url.endsWith(".m3u8")

internal expect fun formatMinSec(value: Int): String

internal expect fun formatInterval(value: Int): Int

internal expect fun formatYoutubeInterval(value: Int): Int

internal fun isHlsUrl(url: String): Boolean =
    url.endsWith(".m3u8") ||
        url.contains("application/vnd.apple.mpegurl")

fun isDesktop(): Boolean = isPlatform() == Platform.Desktop

expect fun isPlatform(): Platform
