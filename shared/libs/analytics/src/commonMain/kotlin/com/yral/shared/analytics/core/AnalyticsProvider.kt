package com.yral.shared.analytics.core

interface AnalyticsProvider {
    val name: String
    fun shouldTrackEvent(event: Event): Boolean
    fun trackEvent(event: Event)
    fun flush()
}
