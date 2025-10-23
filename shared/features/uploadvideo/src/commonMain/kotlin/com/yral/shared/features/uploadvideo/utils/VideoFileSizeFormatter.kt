package com.yral.shared.features.uploadvideo.utils

import com.yral.shared.libs.NumberFormatter

@Suppress("MagicNumber")
internal fun formatFileSize(
    bytes: Long,
    precision: Int = 1,
): String {
    val units = arrayOf("B", "KB", "MB", "GB")
    var size = bytes.toFloat()
    var unitIndex = 0

    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }

    val sizeString =
        NumberFormatter().format(
            size.toDouble(),
            minimumFractionDigits = 0,
            maximumFractionDigits = precision,
        )
    val unit = units[unitIndex]
    return "$sizeString $unit"
}
