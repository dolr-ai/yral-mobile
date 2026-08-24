package com.yral.shared.http.spacetime

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

/**
 * Positional decoder for SpacetimeDB REST responses.
 *
 * SpacetimeDB's REST `/call` endpoint serializes procedure return values using
 * `SerdeWrapper(AlgebraicValue)` which serializes SATS product types (structs)
 * as **positional JSON arrays** — not named-field objects. Sum types (Option,
 * enums) serialize as `[variant_tag, payload]`.
 *
 * This class wraps a `JsonArray` and provides typed accessors by index,
 * matching the field order defined in the Rust `SpacetimeType` structs.
 */
internal class SpacetimePositionalDecoder(private val array: JsonArray) {
    fun getString(index: Int): String =
        (element(index) as JsonPrimitive).content

    fun getStringOrNull(index: Int): String? {
        val element = elementOrNull(index) ?: return null
        if (element is JsonNull) return null
        return (element as JsonPrimitive).content
    }

    fun getBoolean(index: Int): Boolean {
        val element = element(index) as JsonPrimitive
        return element.content.toBooleanStrict()
    }

    fun getBooleanOrNull(index: Int): Boolean? {
        val element = elementOrNull(index) ?: return null
        if (element is JsonNull) return null
        return (element as JsonPrimitive).content.toBooleanStrict()
    }

    fun getULong(index: Int): ULong {
        val element = element(index) as JsonPrimitive
        return element.content.toULong()
    }

    fun getUInt(index: Int): UInt {
        val element = element(index) as JsonPrimitive
        return element.content.toUInt()
    }

    fun getLong(index: Int): Long {
        val element = element(index) as JsonPrimitive
        return element.content.toLong()
    }

    fun getArray(index: Int): JsonArray = element(index) as JsonArray

    fun getArrayOrNull(index: Int): JsonArray? {
        val element = elementOrNull(index) ?: return null
        if (element is JsonNull) return null
        return element as? JsonArray
    }

    fun size(): Int = array.size

    private fun element(index: Int) =
        array[index]

    private fun elementOrNull(index: Int) =
        array.getOrNull(index)
}

/**
 * Parses a SATS sum-type array `[tag, payload]` into its variant tag and
 * payload array.
 *
 * - `Option::Some(T)` → `[0, T_json]`
 * - `Option::None`     → `[1, []]`
 * - `PostStatus::Draft` → `[7, []]`
 * - `SubscriptionPlan::Pro(data)` → `[1, [field0, field1]]`
 */
internal data class SumVariant(val tag: Int, val payload: JsonArray) {
    companion object {
        fun fromArray(array: JsonArray): SumVariant {
            val tag = (array[0] as JsonPrimitive).content.toInt()
            val payload = array[1] as JsonArray
            return SumVariant(tag, payload)
        }
    }
}

/**
 * Parse a SATS `Option<T>` from its sum-type encoding.
 * Returns `null` for `None` (tag 1), or the decoded payload for `Some` (tag 0).
 */
internal fun parseOptionArray(array: JsonArray): JsonArray? {
    val variant = SumVariant.fromArray(array)
    if (variant.tag != 0) return null
    return variant.payload
}

/**
 * Parse a SATS `Vec<String>` from a JSON array of string primitives.
 */
internal fun parseStringVec(array: JsonArray): List<String> =
    array.map { (it as JsonPrimitive).content }

/**
 * Parse a SATS `Vec<Long>` from a JSON array of number primitives (used for
 * `Timestamp` which serializes as `[micros]`).
 */
internal fun parseLongVec(array: JsonArray): List<Long> =
    array.map { (it as JsonPrimitive).content.toLong() }