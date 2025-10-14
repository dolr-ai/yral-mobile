package com.yral.shared.analytics.providers.onesignal

import com.yral.shared.analytics.AnalyticsProvider
import com.yral.shared.analytics.EventToMapConverter
import com.yral.shared.analytics.User
import com.yral.shared.analytics.events.EventData

@Suppress("UnusedPrivateProperty")
class OneSignalAnalyticsProvider(
    eventFilter: (EventData) -> Boolean = { true },
    mapConverter: EventToMapConverter,
    private val oneSignal: OneSignalKMP,
    appId: String,
) : AnalyticsProvider {
    override val name: String = "one_signal"

    init {
        oneSignal.initialize(appId)
    }

    override fun shouldTrackEvent(event: EventData): Boolean = false

    override fun trackEvent(event: EventData) {
        // No-op: OneSignal events are not tracked via analytics pipeline yet.
    }

    override fun setUserProperties(user: User) {
        if (user.isLoggedIn == true) {
            val externalId = user.oneSignalUserId ?: user.userId
            oneSignal.login(externalId)
        } else {
            oneSignal.logout()
        }
    }

    override fun reset() {
        oneSignal.logout()
    }

    override fun toValidKeyName(key: String): String = key
}
