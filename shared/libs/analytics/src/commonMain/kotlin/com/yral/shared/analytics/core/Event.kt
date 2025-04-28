package com.yral.shared.analytics.core

import kotlinx.datetime.Clock
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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull

@Serializable(with = EventSerializer::class)
data class Event(
    val name: String,
    val properties: Map<String, Any> = emptyMap(),
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val featureName: String,
)

class EventSerializer : KSerializer<Event> {
    // Define the serial descriptor
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("Event") {
            element<String>("name")
            element<JsonObject>("properties")
            element<Long>("timestamp")
            element<String>("featureName")
        }

    // Custom encoder for Event class
    override fun serialize(
        encoder: Encoder,
        value: Event,
    ) {
        val jsonEncoder =
            encoder as? JsonEncoder
                ?: throw SerializationException("This serializer can only be used with JSON")
        val json = jsonEncoder.json

        val propertiesJson =
            value.properties.mapValues { (_, v) ->
                when (v) {
                    is Number, is Boolean, is String ->
                        json.parseToJsonElement(
                            json.encodeToString(v),
                        )

                    is List<*> -> JsonArray(v.mapNotNull { item -> serializeAny(item, json) })
                    is Map<*, *> -> {
                        val mapEntries =
                            v
                                .entries
                                .mapNotNull { (key, value) ->
                                    val keyStr = key?.toString() ?: return@mapNotNull null
                                    val valueJson =
                                        serializeAny(value, json)
                                    keyStr to valueJson
                                }.toMap()
                        JsonObject(mapEntries)
                    }

                    else -> {
                        try {
                            // Try to serialize if it's a serializable object
                            json.parseToJsonElement(json.encodeToString(v))
                        } catch (e: SerializationException) {
                            throw IllegalStateException(
                                "Can't serialize type: ${v::class.simpleName}",
                                e,
                            )
                        }
                    }
                }
            }

        val jsonObject =
            buildJsonObject {
                put("name", JsonPrimitive(value.name))
                put("properties", JsonObject(propertiesJson))
                put("timestamp", JsonPrimitive(value.timestamp))
                put("featureName", JsonPrimitive(value.featureName))
            }

        encoder.encodeSerializableValue(JsonObject.serializer(), jsonObject)
    }

    // Helper function to serialize any value to JsonElement
    private fun serializeAny(
        value: Any?,
        json: Json,
    ): JsonElement {
        return when (value) {
            null -> JsonNull
            is Number, is Boolean, is String -> json.parseToJsonElement(json.encodeToString(value))
            is List<*> -> JsonArray(value.mapNotNull { serializeAny(it, json) })
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

    // Custom decoder for Event class
    override fun deserialize(decoder: Decoder): Event {
        val jsonDecoder =
            decoder as? JsonDecoder
                ?: throw SerializationException("This serializer can only be used with JSON")
        val jsonElement = jsonDecoder.decodeJsonElement()

        if (jsonElement !is JsonObject) throw SerializationException("Expected JsonObject")

        val name =
            jsonElement["name"]?.jsonPrimitive?.content
                ?: throw SerializationException("Missing name")
        val timestamp =
            jsonElement["timestamp"]?.jsonPrimitive?.longOrNull ?: Clock.System
                .now()
                .toEpochMilliseconds()
        val featureName =
            jsonElement["featureName"]?.jsonPrimitive?.content
                ?: throw SerializationException("Missing featureName")

        val propertiesJson =
            jsonElement["properties"] as? JsonObject ?: JsonObject(emptyMap())
        val properties =
            propertiesJson.mapValues { (_, jsonValue) ->
                deserializeJsonElement(jsonValue)
            }

        return Event(
            name = name,
            properties = properties,
            timestamp = timestamp,
            featureName = featureName,
        )
    }

    // Helper function to deserialize JsonElement to appropriate Kotlin types
    private fun deserializeJsonElement(element: JsonElement): Any =
        when (element) {
            is JsonNull -> Unit
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
