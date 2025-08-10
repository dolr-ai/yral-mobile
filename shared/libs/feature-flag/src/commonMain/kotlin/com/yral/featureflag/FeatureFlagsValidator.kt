package com.yral.featureflag

import com.yral.featureflag.core.FeatureFlag

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<ValidationError>,
)

sealed class ValidationError(
    open val key: String,
    open val message: String,
) {
    data class DuplicateKey(
        override val key: String,
        val occurrences: Int,
    ) : ValidationError(key, "Duplicate key '$key' found $occurrences times")

    data class CodecRoundTripFailed(
        override val key: String,
    ) : ValidationError(key, "Codec round-trip failed for key '$key'")

    data class EmptyMetadata(
        override val key: String,
        val field: String,
    ) : ValidationError(key, "Missing or empty $field for key '$key'")

    data class PrefixPolicyViolation(
        override val key: String,
        val allowed: List<String>?,
    ) : ValidationError(
            key,
            if (allowed.isNullOrEmpty()) {
                "Key '$key' must contain a '.' prefix separator"
            } else {
                "Key '$key' does not start with any allowed prefixes: ${allowed.joinToString()}"
            },
        )
}

object FeatureFlagsValidator {
    fun validate(
        flags: List<FeatureFlag<*>>,
        allowedPrefixes: List<String>? = null,
        enforceDotSeparatorWhenNoPrefixes: Boolean = true,
        strictRoundTripEquality: Boolean = false,
    ): ValidationResult {
        val errors = mutableListOf<ValidationError>()

        // 1) Duplicate keys
        flags
            .groupBy { it.key }
            .filter { it.value.size > 1 }
            .forEach { (key, list) -> errors += ValidationError.DuplicateKey(key, list.size) }

        // 2) Metadata
        flags.forEach { flag ->
            if (flag.name.isBlank()) errors += ValidationError.EmptyMetadata(flag.key, "name")
            if (flag.description.isBlank()) errors += ValidationError.EmptyMetadata(flag.key, "description")
        }

        // 3) Codec round-trip on defaults
        flags.forEach { flag ->
            @Suppress("UNCHECKED_CAST")
            val anyFlag = flag as FeatureFlag<Any?>
            val enc = anyFlag.codec.encode(anyFlag.defaultValue)
            val dec = anyFlag.codec.decode(enc)
            val failed = dec == null || (strictRoundTripEquality && dec != anyFlag.defaultValue)
            if (failed) errors += ValidationError.CodecRoundTripFailed(anyFlag.key)
        }

        // 4) Key prefix policy
        flags.forEach { flag ->
            val ok =
                when {
                    !allowedPrefixes.isNullOrEmpty() -> allowedPrefixes.any { prefix -> flag.key.startsWith(prefix) }
                    enforceDotSeparatorWhenNoPrefixes -> flag.key.contains('.')
                    else -> true
                }
            if (!ok) errors += ValidationError.PrefixPolicyViolation(flag.key, allowedPrefixes)
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    fun validateOrThrow(
        flags: List<FeatureFlag<*>>,
        allowedPrefixes: List<String>? = null,
        enforceDotSeparatorWhenNoPrefixes: Boolean = true,
        strictRoundTripEquality: Boolean = false,
    ) {
        val result = validate(flags, allowedPrefixes, enforceDotSeparatorWhenNoPrefixes, strictRoundTripEquality)
        if (!result.isValid) {
            val msg =
                buildString {
                    appendLine("Feature flag validation failed with ${result.errors.size} error(s):")
                    result.errors.forEach { appendLine(" - ${it.message}") }
                }
            throw IllegalStateException(msg)
        }
    }

    fun validateOrThrow(vararg groups: List<FeatureFlag<*>>) {
        val all = groups.toList().flatMap { it }
        validateOrThrow(all)
    }
}
