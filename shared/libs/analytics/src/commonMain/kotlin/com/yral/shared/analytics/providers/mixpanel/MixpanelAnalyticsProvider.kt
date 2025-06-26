package com.yral.shared.analytics.providers.mixpanel

import com.yral.shared.analytics.AnalyticsProvider
import com.yral.shared.analytics.events.EventData

expect class MixpanelAnalyticsProvider(
    eventFilter: (EventData) -> Boolean = { true },
    mapConverter: (EventData) -> Map<String, Any>,
    token: String,
) : AnalyticsProvider
