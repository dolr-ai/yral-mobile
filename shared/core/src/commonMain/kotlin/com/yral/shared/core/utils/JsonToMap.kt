package com.yral.shared.core.utils

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

fun JsonElement.toMap(): Map<String, Any?> =
    if (this !is JsonObject) {
        emptyMap()
    } else {
        entries.associate { (key, element) ->
            key to
                when (element) {
                    is JsonObject -> element.toMap()
                    else -> element
                }
        }
    }
