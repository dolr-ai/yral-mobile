package com.yral.shared.core.utils

private const val USERNAME_DIGIT_COUNT = 3
private const val USERNAME_MAX_LENGTH = 15

internal expect fun sha256(data: ByteArray): ByteArray

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

@Suppress("MagicNumber")
fun generateUsernameFromPrincipal(principal: String): String {
    val seed = sha256(principal.encodeToByteArray())
    val generator = SeededGenerator(seed)
    val noun = USERNAME_NOUNS.randomOrDefault(generator, "apple")
    val adjective = USERNAME_ADJECTIVES.randomOrDefault(generator, "bold")

    val base =
        StringBuilder(noun.length + adjective.length + USERNAME_DIGIT_COUNT).apply {
            append(noun)
            append(adjective)
            val limit = (USERNAME_MAX_LENGTH - USERNAME_DIGIT_COUNT).coerceAtLeast(0)
            if (length > limit) {
                setLength(limit)
            }
        }

    repeat(USERNAME_DIGIT_COUNT) {
        base.append(generator.nextInt(10))
    }

    return base.toString()
}

@Suppress("MagicNumber")
private class SeededGenerator(
    seedBytes: ByteArray,
) {
    private val state =
        if (seedBytes.size >= 32) {
            seedBytes.copyOf(32)
        } else {
            ByteArray(32).also { seedBytes.copyInto(it, endIndex = seedBytes.size) }
        }
    private var index = 0

    private fun nextChunk(): Long {
        if (index + 8 > state.size) {
            index = 0
        }
        var value = 0L
        repeat(8) { offset ->
            value = (value shl 8) or (state[index + offset].toLong() and 0xffL)
        }
        index += 8
        return value
    }

    fun nextInt(bound: Int): Int {
        require(bound > 0)
        val next = nextChunk() and Long.MAX_VALUE
        return (next % bound.toLong()).toInt()
    }
}

private fun List<String>.randomOrDefault(
    generator: SeededGenerator,
    fallback: String,
): String =
    if (isEmpty()) {
        fallback
    } else {
        this[generator.nextInt(size)]
    }
