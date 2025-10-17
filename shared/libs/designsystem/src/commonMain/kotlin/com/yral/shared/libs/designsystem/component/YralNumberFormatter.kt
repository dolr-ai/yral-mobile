package com.yral.shared.libs.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.yral.shared.libs.NumberFormatter

@Composable
fun formatAbbreviation(
    num: Int,
    decimals: Int = 1,
): String = formatAbbreviation(num.toDouble(), decimals)

@Composable
fun formatAbbreviation(
    num: Long,
    decimals: Int = 1,
): String = formatAbbreviation(num.toDouble(), decimals)

@Suppress("MagicNumber")
@Composable
fun formatAbbreviation(
    number: Double,
    decimals: Int,
): String {
    val numberFormatter = remember { NumberFormatter() }
    var current = number
    var index = 0
    while (current >= 1000 && index < prefixes.size - 1) {
        current /= 1000
        index++
    }
    return "${numberFormatter.format(value = current, maximumFractionDigits = decimals)}${prefixes[index]}"
}

private val prefixes = arrayOf("", "K", "M", "B", "T")
