@file:Suppress("MagicNumber")

package com.yral.shared.libs.videoPlayer.util

import com.yral.shared.libs.videoPlayer.model.Platform

internal actual fun formatMinSec(value: Int): String = "STUB"

internal actual fun formatInterval(value: Int): Int = value * 1000 // STUB

internal actual fun formatYoutubeInterval(value: Int): Int = value * 1000 // STUB

actual fun isPlatform(): Platform = Platform.Ios
