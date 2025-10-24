package com.yral.shared.analytics.providers.facebook

import android.content.Context
import com.facebook.appevents.AppEventsLogger
import com.yral.shared.analytics.AnalyticsProvider
import com.yral.shared.analytics.EventToMapConverter
import com.yral.shared.analytics.User
import com.yral.shared.analytics.events.EventData
import com.yral.shared.analytics.toBundle
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual class FacebookAnalyticsProvider actual constructor(
    private val eventFilter: (EventData) -> Boolean,
    private val mapConverter: EventToMapConverter,
) : AnalyticsProvider,
    KoinComponent {
    private val context: Context by inject()
    override val name: String = "facebook"

    override fun shouldTrackEvent(event: EventData): Boolean = eventFilter(event)

    override fun trackEvent(event: EventData) {
        val logger = AppEventsLogger.newLogger(context)
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

    override fun reset(resetOnlyProperties: Boolean) {
        AppEventsLogger.clearUserID()
    }

    override fun toValidKeyName(key: String): String = key
}
