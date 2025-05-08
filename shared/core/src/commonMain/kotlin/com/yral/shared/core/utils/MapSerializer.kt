package com.yral.shared.core.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull

class MapSerializer : KSerializer<Map<String, Any?>> {
    // Define the serial descriptor
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("Map") {
            element<JsonObject>("map")
        }

    // Custom encoder for Map<String, Any>
    override fun serialize(
        encoder: Encoder,
        value: Map<String, Any?>,
    ) {
        val jsonEncoder =
            encoder as? JsonEncoder
                ?: throw SerializationException("This serializer can only be used with JSON")
        val json = jsonEncoder.json

        val propertiesJson =
            value.mapValues { (_, v) ->
                serializeAny(v, json)
            }

        encoder.encodeSerializableValue(JsonObject.serializer(), JsonObject(propertiesJson))
    }

    // Helper function to serialize any value to JsonElement
    private fun serializeAny(
        value: Any?,
        json: Json,
    ): JsonElement {
        return when (value) {
            null -> JsonNull
            is Number, is Boolean, is String ->
                json.parseToJsonElement(
                    json.encodeToString(value),
                )

            is List<*> -> JsonArray(value.mapNotNull { item -> serializeAny(item, json) })
            is Map<*, *> -> {
                val mapEntries =
                    value
                        .entries
                        .mapNotNull { (key, v) ->
                            val keyStr = key?.toString() ?: return@mapNotNull null
                            val valueJson = serializeAny(v, json)
                            keyStr to valueJson
                        }.toMap()
                JsonObject(mapEntries)
            }

            else -> {
                try {
                    // Try to serialize if it's a serializable object
                    json.parseToJsonElement(json.encodeToString(value))
                } catch (e: SerializationException) {
                    throw IllegalStateException(
                        "Can't serialize type: ${value::class.simpleName}",
                        e,
                    )
                }
            }
        }
    }

    // Custom decoder for Map<String, Any>
    override fun deserialize(decoder: Decoder): Map<String, Any?> {
        val jsonDecoder =
            decoder as? JsonDecoder
                ?: throw SerializationException("This serializer can only be used with JSON")
        return when (val jsonElement = jsonDecoder.decodeJsonElement()) {
            is JsonObject ->
                jsonElement.mapValues { (_, jsonValue) ->
                    deserializeJsonElement(jsonValue)
                }

            else -> throw SerializationException("Expected JsonObject for Map deserialization")
        }
    }

    // Helper function to deserialize JsonElement to appropriate Kotlin types
    private fun deserializeJsonElement(element: JsonElement): Any? =
        when (element) {
            is JsonNull -> null
            is JsonPrimitive -> {
                when {
                    element.isString -> element.content
                    element.booleanOrNull != null -> element.boolean
                    element.longOrNull != null -> element.long
                    element.doubleOrNull != null -> element.double
                    else -> element.content
                }
            }

            is JsonArray -> element.map { deserializeJsonElement(it) }
            is JsonObject -> element.mapValues { (_, value) -> deserializeJsonElement(value) }
        }
}

/**
 * A wrapper class for Map<String, Any?> that can be serialized using kotlinx.serialization.
 * This class implements Map interface by delegating all its methods to the underlying map.
 *
 * Example usage:
 * ```
 * val map = SerializableMap(mapOf("key" to "value", "number" to 42))
 * val json = Json.encodeToString(map)
 * val deserialized = Json.decodeFromString<SerializableMap>(json)
 * ```
 */
@Serializable(with = MapSerializer::class)
class SerializableMap(
    private val map: Map<String, Any?> = emptyMap(),
) : Map<String, Any?> by map {
    override fun equals(other: Any?): Boolean = map == other
    override fun hashCode(): Int = map.hashCode()
    override fun toString(): String = map.toString()

    // Convenience constructor for creating from pairs
    @Suppress("SpreadOperator")
    constructor(vararg pairs: Pair<String, Any?>) : this(mapOf(*pairs))

    // Convert to regular Map
    fun toMap(): Map<String, Any?> = map
}

/**
 * Extension function to convert a regular Map<String, Any?> to SerializableMap
 */
fun Map<String, Any?>.toSerializableMap(): SerializableMap = SerializableMap(this)
