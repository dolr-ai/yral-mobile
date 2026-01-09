package com.yral.shared.analytics.providers.branch

import com.yral.shared.analytics.AnalyticsProvider
import com.yral.shared.analytics.EventToMapConverter
import com.yral.shared.analytics.events.EventData

expect class BranchAnalyticsProvider(
    eventFilter: (EventData) -> Boolean = { true },
    mapConverter: EventToMapConverter,
) : AnalyticsProvider
