package com.yral.shared.features.leaderboard.ui.main

import androidx.compose.ui.text.intl.Locale
import kotlin.time.Duration.Companion.milliseconds

@Suppress("MagicNumber")
internal actual fun formatMillisToHHmmSS(millis: Long): String {
    val duration = millis.milliseconds
    val hours = duration.inWholeHours
    val minutes = (duration.inWholeMinutes % 60)
    val seconds = (duration.inWholeSeconds % 60)
    return String.format(Locale.current.platformLocale, "%02d:%02d:%02d", hours, minutes, seconds)
}
