package com.yral.shared.analytics.providers.mixpanel

import com.yral.shared.analytics.AnalyticsProvider
import com.yral.shared.analytics.EventToMapConverter
import com.yral.shared.analytics.events.EventData
import com.yral.shared.preferences.UtmAttributionStore

expect class MixpanelAnalyticsProvider(
    eventFilter: (EventData) -> Boolean = { true },
    mapConverter: EventToMapConverter,
    token: String,
    utmAttributionStore: UtmAttributionStore,
) : AnalyticsProvider
