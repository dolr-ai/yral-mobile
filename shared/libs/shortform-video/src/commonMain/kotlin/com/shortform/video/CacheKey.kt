package com.shortform.video

fun MediaDescriptor.cacheKey(): String {
    val headersKey = headers.entries.sortedBy { it.key }.joinToString("&") { (key, value) ->
        "$key=$value"
    }
    val base = buildString {
        append(uri)
        append('|')
        append(containerHint.name)
        append('|')
        append(headersKey)
    }
    return fnv1a64Hex(base)
}

private fun fnv1a64Hex(value: String): String {
    var hash = 14695981039346656037UL
    val prime = 1099511628211UL
    val bytes = value.encodeToByteArray()
    for (byte in bytes) {
        hash = hash xor byte.toUByte().toULong()
        hash *= prime
    }
    return hash.toString(16)
}
