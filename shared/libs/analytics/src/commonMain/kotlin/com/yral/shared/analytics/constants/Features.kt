package com.yral.shared.analytics.constants

enum class Features {
    AUTH,
    FEED,
    ;

    fun getFeatureName(): String = name.lowercase()
}
