package com.yral.shared.analytics

import android.os.Bundle

internal fun toBundle(map: Map<String, Any>): Bundle =
    Bundle().apply {
        map.forEach { (key, value) ->
            when (value) {
                is Int -> putInt(key, value)
                is Long -> putLong(key, value)
                is Double -> putDouble(key, value)
                is Float -> putFloat(key, value)
                else -> putString(key, value.toString())
            }
        }
    }
