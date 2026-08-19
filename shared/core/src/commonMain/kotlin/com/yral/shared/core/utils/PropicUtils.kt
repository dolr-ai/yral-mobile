package com.yral.shared.core.utils

/**
 * Pure Kotlin profile picture URL generation.
 *
 * Generates a default profile picture URL from a principal string by:
 * 1. Computing CRC32 of the principal's UTF-8 bytes
 * 2. Taking `crc32 % 18557 + 1` as an index
 * 3. Building the Hetzner GobGob URL: `https://prakash-yral.hel1.your-objectstorage.com/gobgob/gob.<index>.png`
 *
 * This replaces the Rust FFI `propicFromPrincipal` + `rewriteGobUrl` chain.
 * The GobGob avatar index is deterministic per principal.
 */
private const val GOBGOB_TOTAL_COUNT = 18557
private const val HETZNER_GOB_URL_PREFIX =
    "https://prakash-yral.hel1.your-objectstorage.com/gobgob/gob."

fun propicFromPrincipal(principalId: String): String {
    val hash = crc32(principalId.encodeToByteArray())
    val index = (hash % GOBGOB_TOTAL_COUNT) + 1
    return "$HETZNER_GOB_URL_PREFIX$index.png"
}

/**
 * Kotlin implementation of CRC32 (IEEE 802.3 polynomial).
 * Used for deterministic GobGob avatar index assignment.
 */
@Suppress("MagicNumber")
private fun crc32(data: ByteArray): Int {
    val table =
        IntArray(256) { i ->
            var crc = i
            repeat(8) {
                crc =
                    if (crc and 1 != 0) {
                        (crc ushr 1) xor 0xEDB88320.toInt()
                    } else {
                        crc ushr 1
                    }
            }
            crc
        }
    var crc = 0xFFFFFFFF.toInt()
    for (b in data) {
        val index = (crc xor (b.toInt() and 0xFF)) and 0xFF
        crc = (crc ushr 8) xor table[index]
    }
    return crc xor 0xFFFFFFFF.toInt()
}
