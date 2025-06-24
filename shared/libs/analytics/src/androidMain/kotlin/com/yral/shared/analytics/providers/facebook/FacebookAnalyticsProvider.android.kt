package com.yral.shared.analytics.providers.facebook

import co.touchlab.kermit.Logger
import com.yral.shared.analytics.AnalyticsProvider
import com.yral.shared.analytics.User
import com.yral.shared.analytics.events.EventData

actual class FacebookAnalyticsProvider actual constructor(
    eventFilter: (EventData) -> Boolean,
    mapConverter: (EventData) -> Map<String, Any>,
) : AnalyticsProvider {
    override val name: String
        get() = "facebook"

    override fun shouldTrackEvent(event: EventData): Boolean = false

    override fun trackEvent(event: EventData) {
        Logger.d("Facebook not integrated")
    }

    override fun setUserProperties(user: User) {
        Logger.d("Facebook not integrated")
    }

    override fun reset() {
        Logger.d("Facebook not integrated")
    }

    override fun toValidKeyName(key: String): String = key
}
