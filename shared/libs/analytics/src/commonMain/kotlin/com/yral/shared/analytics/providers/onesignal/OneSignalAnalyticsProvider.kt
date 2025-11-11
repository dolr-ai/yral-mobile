package com.yral.shared.analytics.providers.onesignal

import co.touchlab.kermit.Logger
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

    private val logger = Logger.withTag("OneSignalAnalytics")
    private val isInitialized =
        runCatching {
            require(appId.isNotBlank()) { "OneSignal app id must not be blank" }
            oneSignal.initialize(appId)
            true
        }.onFailure { error ->
            logger.e(error) { "Failed to initialize OneSignal" }
        }.getOrDefault(false)

    override fun shouldTrackEvent(event: EventData): Boolean = false

    override fun trackEvent(event: EventData) {
        // No-op: OneSignal events are not tracked via analytics pipeline yet.
    }

    override fun setUserProperties(user: User) {
        when (user.isLoggedIn) {
            true -> runWhenInitialized { oneSignal.login(user.userId) }
            false -> runWhenInitialized { oneSignal.logout() }
            null -> logger.v { "Skipping OneSignal user sync because login state is unknown" }
        }
    }

    override fun reset() {
        runWhenInitialized { oneSignal.logout() }
    }

    override fun toValidKeyName(key: String): String = key

    private inline fun runWhenInitialized(block: () -> Unit) {
        if (isInitialized) {
            block()
        } else {
            logger.w { "OneSignal not initialized; skipping requested operation" }
        }
    }
}
