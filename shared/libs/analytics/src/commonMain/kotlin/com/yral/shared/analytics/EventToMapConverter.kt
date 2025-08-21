package com.yral.shared.analytics

import com.yral.shared.analytics.events.EventData
import com.yral.shared.core.utils.toMap
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull

class EventToMapConverter(
    private val json: Json,
) {
    fun toMap(event: EventData): Map<String, Any> =
        json
            .encodeToJsonElement(event)
            .toMap()
            .filterKeys { it != "type" }
            .mapKeys { (key, _) -> if (key == "type_ext") "type" else key }
            .filterKeys { it != "type_ext" }
            .mapValues { (_, value) ->
                when (value) {
                    is JsonPrimitive ->
                        when {
                            value.isString -> value.content
                            value.booleanOrNull != null -> value.boolean
                            value.intOrNull != null -> value.int
                            value.longOrNull != null -> value.long
                            value.doubleOrNull != null -> value.double
                            else -> value.content
                        }
                    else -> value.toString()
                }
            }.filterValues { it != "null" }
            .mapValues { it.value as Any }
}
