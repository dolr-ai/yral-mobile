package com.yral.shared.core.utils

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

fun JsonElement.jsonObjectToMap(): Map<String, Any?> =
    if (this !is JsonObject) {
        emptyMap()
    } else {
        entries.associate { (key, element) ->
            key to
                when (element) {
                    is JsonObject -> element.jsonObjectToMap()
                    else -> element.jsonPrimitive.content
                }
        }
    }
