package com.yral.shared.analytics.providers.snowplow

import com.yral.shared.analytics.AnalyticsProvider
import com.yral.shared.analytics.EventToMapConverter
import com.yral.shared.analytics.User
import com.yral.shared.analytics.events.EventData

actual class SnowplowAnalyticsProvider actual constructor(
    private val eventFilter: (EventData) -> Boolean,
    private val mapConverter: EventToMapConverter,
) : AnalyticsProvider {
    override val name: String = "snowplow"

    override fun shouldTrackEvent(event: EventData): Boolean = false

    override fun trackEvent(event: EventData) {}

    override fun setUserProperties(user: User) {}

    override fun reset() {}

    override fun toValidKeyName(key: String): String = key
}
