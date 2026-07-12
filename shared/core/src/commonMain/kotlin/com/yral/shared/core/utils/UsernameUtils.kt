package com.yral.shared.core.utils

private const val USERNAME_MAX_LENGTH = 15
private const val USERNAME_GENERATION_ATTEMPTS = 128

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
    repeat(USERNAME_GENERATION_ATTEMPTS) {
        val firstModifier = USERNAME_MODIFIERS.randomOrDefault(generator, "cute")
        val secondModifier = USERNAME_MODIFIERS.randomDistinctOrDefault(generator, firstModifier, "kind")
        val noun = USERNAME_NOUNS.randomOrDefault(generator, "panda")
        val username = firstModifier + secondModifier + noun
        if (username.length <= USERNAME_MAX_LENGTH) {
            return username
        }
    }

    return DEFAULT_SAFE_USERNAME
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
            val nextState = sha256(state)
            nextState.copyInto(state)
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

private fun List<String>.randomDistinctOrDefault(
    generator: SeededGenerator,
    excluded: String,
    fallback: String,
): String {
    val excludedIndex = indexOf(excluded)
    return when {
        isEmpty() -> fallback
        size == 1 -> first()
        excludedIndex == -1 -> randomOrDefault(generator, fallback)
        else -> {
            val randomIndex = generator.nextInt(size - 1)
            val adjustedIndex = if (randomIndex >= excludedIndex) randomIndex + 1 else randomIndex
            this[adjustedIndex]
        }
    }
}

private const val DEFAULT_SAFE_USERNAME = "cutekindpanda"
