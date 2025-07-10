package com.yral.shared.analytics.providers.facebook

import com.yral.shared.analytics.AnalyticsProvider
import com.yral.shared.analytics.EventToMapConverter
import com.yral.shared.analytics.events.EventData

expect class FacebookAnalyticsProvider(
    eventFilter: (EventData) -> Boolean = { true },
    mapConverter: EventToMapConverter,
) : AnalyticsProvider
