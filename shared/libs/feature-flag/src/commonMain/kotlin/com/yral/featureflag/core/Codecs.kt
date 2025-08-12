package com.yral.featureflag.core

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

interface FlagCodec<T> {
    fun encode(value: T): String
    fun decode(raw: String): T?
}

object BooleanCodec : FlagCodec<Boolean> {
    private val truthy = setOf("true", "1", "yes", "on")
    private val falsy = setOf("false", "0", "no", "off")
    override fun encode(value: Boolean): String = value.toString()
    override fun decode(raw: String): Boolean? {
        val v = raw.trim().lowercase()
        return when (v) {
            in truthy -> true
            in falsy -> false
            else -> null
        }
    }
}

object StringCodec : FlagCodec<String> {
    override fun encode(value: String): String = value
    override fun decode(raw: String): String = raw
}

object IntCodec : FlagCodec<Int> {
    override fun encode(value: Int): String = value.toString()
    override fun decode(raw: String): Int? = raw.trim().toIntOrNull()
}

object LongCodec : FlagCodec<Long> {
    override fun encode(value: Long): String = value.toString()
    override fun decode(raw: String): Long? = raw.trim().toLongOrNull()
}

object DoubleCodec : FlagCodec<Double> {
    override fun encode(value: Double): String = value.toString()
    override fun decode(raw: String): Double? = raw.trim().toDoubleOrNull()
}

class EnumCodec<E : Enum<E>>(
    val enumValues: Array<E>,
) : FlagCodec<E> {
    override fun encode(value: E): String = value.name
    override fun decode(raw: String): E? = enumValues.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) }
}

class JsonCodec<T>(
    private val serializer: KSerializer<T>,
) : FlagCodec<T> {
    override fun encode(value: T): String = FeatureFlagsJson.encodeToString(serializer, value)
    override fun decode(raw: String): T? = runCatching { FeatureFlagsJson.decodeFromString(serializer, raw) }.getOrNull()
}

class StringListCodec : FlagCodec<List<String>> {
    private val delimiter = ","
    override fun encode(value: List<String>): String = value.joinToString(delimiter)
    override fun decode(raw: String): List<String> = if (raw.isEmpty()) emptyList() else raw.split(delimiter).map { it.trim() }
}

class StringMapCodec : FlagCodec<Map<String, String>> {
    // JSON encoding for robustness
    private val serializer = MapSerializer(String.serializer(), String.serializer())
    override fun encode(value: Map<String, String>): String = FeatureFlagsJson.encodeToString(serializer, value)
    override fun decode(raw: String): Map<String, String>? = runCatching { FeatureFlagsJson.decodeFromString(serializer, raw) }.getOrNull()
}
