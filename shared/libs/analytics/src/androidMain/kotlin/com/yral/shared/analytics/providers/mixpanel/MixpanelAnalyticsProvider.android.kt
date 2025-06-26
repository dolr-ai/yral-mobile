package com.yral.shared.analytics.providers.mixpanel

import com.yral.shared.analytics.AnalyticsProvider
import com.yral.shared.analytics.User
import com.yral.shared.analytics.events.EventData

actual class MixpanelAnalyticsProvider actual constructor(
    eventFilter: (EventData) -> Boolean,
    mapConverter: (EventData) -> Map<String, Any>,
    token: String,
) : AnalyticsProvider {
    override val name: String = "mixpanel"

    override fun shouldTrackEvent(event: EventData): Boolean {
        TODO("Not yet implemented")
    }

    override fun trackEvent(event: EventData) {
        TODO("Not yet implemented")
    }

    override fun setUserProperties(user: User) {
        TODO("Not yet implemented")
    }

    override fun reset() {
        TODO("Not yet implemented")
    }

    override fun toValidKeyName(key: String): String {
        TODO("Not yet implemented")
    }
}
