package com.yral.shared.core

interface AnalyticsProvider {
    val name: String
    fun shouldTrackEvent(event: Event): Boolean
    fun trackEvent(event: Event)
    fun flush()
}
