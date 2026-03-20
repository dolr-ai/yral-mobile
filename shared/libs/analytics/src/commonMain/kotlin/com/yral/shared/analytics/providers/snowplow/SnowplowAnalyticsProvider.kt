package com.yral.shared.analytics.providers.snowplow

import com.yral.shared.analytics.AnalyticsProvider
import com.yral.shared.analytics.EventToMapConverter
import com.yral.shared.analytics.events.EventData

expect class SnowplowAnalyticsProvider(
    eventFilter: (EventData) -> Boolean = { true },
    mapConverter: EventToMapConverter,
) : AnalyticsProvider
