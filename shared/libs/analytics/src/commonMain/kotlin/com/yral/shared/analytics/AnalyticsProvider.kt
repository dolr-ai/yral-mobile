package com.yral.shared.analytics

import com.yral.shared.analytics.events.EventData

interface AnalyticsProvider {
    val name: String

    fun shouldTrackEvent(event: EventData): Boolean
    fun trackEvent(event: EventData)
    fun flush() { }

    fun setUserProperties(user: User)
    fun reset(resetOnlyProperties: Boolean = false)

    fun toValidKeyName(key: String): String
}
