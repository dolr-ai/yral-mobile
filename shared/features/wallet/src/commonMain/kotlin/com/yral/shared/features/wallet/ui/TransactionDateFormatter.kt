@file:Suppress("MagicNumber")

package com.yral.shared.features.wallet.ui

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime

internal const val PAISE_PER_RUPEE = 100

private val MONTH_NAMES =
    listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

private const val ORDINAL_TEEN_START = 11
private const val ORDINAL_TEEN_END = 13
private const val ORDINAL_DIVISOR = 10

internal fun getOrdinalSuffix(day: Int): String =
    when {
        day in ORDINAL_TEEN_START..ORDINAL_TEEN_END -> "th"
        day % ORDINAL_DIVISOR == 1 -> "st"
        day % ORDINAL_DIVISOR == 2 -> "nd"
        day % ORDINAL_DIVISOR == 3 -> "rd"
        else -> "th"
    }

@OptIn(ExperimentalTime::class)
internal fun formatTransactionDate(
    createdAt: String,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): String =
    try {
        val instant = Instant.parse(createdAt)
        val dt = instant.toLocalDateTime(timeZone)
        val day = dt.dayOfMonth
        val month = MONTH_NAMES[dt.monthNumber - 1]
        val year = dt.year
        val hour = (dt.hour + 11) % 12 + 1
        val minute = dt.minute.toString().padStart(2, '0')
        val amPm = if (dt.hour < 12) "AM" else "PM"
        "$day${getOrdinalSuffix(day)} $month $year, $hour:$minute $amPm"
    } catch (_: Exception) {
        createdAt
    }
