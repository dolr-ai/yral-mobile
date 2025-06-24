package com.yral.shared.analytics.providers.facebook

import com.yral.shared.analytics.AnalyticsProvider
import com.yral.shared.analytics.User
import com.yral.shared.analytics.events.EventData

actual class FacebookAnalyticsProvider actual constructor(
    eventFilter: (EventData) -> Boolean,
    mapConverter: (EventData) -> Map<String, Any>,
) : AnalyticsProvider {
    override val name: String
        get() = TODO("Not yet implemented")

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
