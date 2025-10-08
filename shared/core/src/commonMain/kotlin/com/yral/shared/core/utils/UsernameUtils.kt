package com.yral.shared.core.utils

private const val USERNAME_PREFIX = "player-"
private const val USERNAME_HASH_LENGTH = 10
private const val USERNAME_BASE = 36
private const val USERNAME_PAD_CHAR = '0'

private const val FNV64_OFFSET_BASIS = 0xcbf29ce484222325uL
private const val FNV64_PRIME = 0x100000001b3uL

fun resolveUsername(
    preferred: String?,
    principal: String?,
): String? {
    val sanitized = preferred?.trim()?.takeIf { it.isNotEmpty() }
    if (sanitized != null) {
        return sanitized
    }
    return principal?.let { generateUsernameFromPrincipal(it) }
}

fun generateUsernameFromPrincipal(principal: String): String {
    val hash =
        principal.encodeToByteArray().fold(FNV64_OFFSET_BASIS) { acc, byte ->
            (acc xor byte.toUByte().toULong()) * FNV64_PRIME
        }
    val base36 = hash.toString(USERNAME_BASE)
    val suffix = base36.takeLast(USERNAME_HASH_LENGTH).padStart(USERNAME_HASH_LENGTH, USERNAME_PAD_CHAR)
    return USERNAME_PREFIX + suffix.lowercase()
}
