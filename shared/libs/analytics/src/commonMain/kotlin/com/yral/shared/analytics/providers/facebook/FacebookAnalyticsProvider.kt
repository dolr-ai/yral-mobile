package com.yral.shared.analytics.providers.facebook

import com.yral.shared.analytics.AnalyticsProvider
import com.yral.shared.analytics.EventToMapConverter
import com.yral.shared.analytics.events.EventData
import com.yral.shared.core.platform.PlatformResources

expect class FacebookAnalyticsProvider(
    platformResources: PlatformResources,
    eventFilter: (EventData) -> Boolean = { true },
    mapConverter: EventToMapConverter,
) : AnalyticsProvider
