package com.yral.shared.analytics.providers.facebook

import com.facebook.appevents.AppEventsLogger
import com.yral.shared.analytics.AnalyticsProvider
import com.yral.shared.analytics.EventToMapConverter
import com.yral.shared.analytics.User
import com.yral.shared.analytics.events.EventData
import com.yral.shared.analytics.toBundle
import com.yral.shared.core.platform.PlatformResources

actual class FacebookAnalyticsProvider actual constructor(
    private val platformResources: PlatformResources,
    private val eventFilter: (EventData) -> Boolean,
    private val mapConverter: EventToMapConverter,
) : AnalyticsProvider {
    override val name: String = "facebook"

    override fun shouldTrackEvent(event: EventData): Boolean = eventFilter(event)

    override fun trackEvent(event: EventData) {
        val logger = AppEventsLogger.newLogger(platformResources.applicationContext)
        val map = mapConverter.toMap(event)
        val parameters = toBundle(map)
        logger.logEvent(
            eventName = toValidKeyName(event.event),
            parameters = parameters,
        )
    }

    override fun setUserProperties(user: User) {
        AppEventsLogger.setUserID(user.userId)
    }

    override fun reset() {
        AppEventsLogger.clearUserID()
    }

    override fun toValidKeyName(key: String): String = key
}
