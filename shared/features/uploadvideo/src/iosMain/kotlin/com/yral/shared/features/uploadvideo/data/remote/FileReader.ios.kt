package com.yral.shared.features.uploadvideo.data.remote

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.dataWithContentsOfFile
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
internal actual fun readFileBytes(filePath: String): ByteArray {
    val data =
        NSData.dataWithContentsOfFile(filePath)
            ?: error("Cannot read file: $filePath")
    return ByteArray(data.length.toInt()).also { bytes ->
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), data.bytes, data.length)
        }
    }
}
