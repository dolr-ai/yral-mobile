package com.yral.shared.core.utils

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH

@OptIn(ExperimentalForeignApi::class)
internal actual fun sha256(data: ByteArray): ByteArray {
    val digest = UByteArray(CC_SHA256_DIGEST_LENGTH.toInt())
    data.usePinned { inputPinned ->
        digest.usePinned { digestPinned ->
            // `addressOf(0)` throws when array is empty; pass null pointer instead.
            val dataPointer = if (data.isNotEmpty()) inputPinned.addressOf(0) else null
            CC_SHA256(
                dataPointer,
                data.size.toUInt(),
                digestPinned.addressOf(0),
            )
        }
    }
    return ByteArray(digest.size) { index -> digest[index].toByte() }
}
