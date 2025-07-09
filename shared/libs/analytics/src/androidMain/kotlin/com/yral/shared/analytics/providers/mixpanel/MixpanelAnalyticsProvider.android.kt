package com.yral.shared.analytics.providers.mixpanel

import co.touchlab.kermit.Logger
import com.yral.shared.analytics.AnalyticsProvider
import com.yral.shared.analytics.User
import com.yral.shared.analytics.events.EventData

actual class MixpanelAnalyticsProvider actual constructor(
    eventFilter: (EventData) -> Boolean,
    mapConverter: (EventData) -> Map<String, Any>,
    token: String,
) : AnalyticsProvider {
    override val name: String = "mixpanel"

    override fun shouldTrackEvent(event: EventData): Boolean = false

    override fun trackEvent(event: EventData) {
        Logger.d("MixPanel not integrated")
    }

    override fun setUserProperties(user: User) {
        Logger.d("MixPanel not integrated")
    }

    override fun reset() {
        Logger.d("MixPanel not integrated")
    }

    override fun toValidKeyName(key: String): String = key
}
