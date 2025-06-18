package com.yral.shared.analytics.providers.facebook

import cocoapods.FBSDKCoreKit.FBSDKAppEvents
import com.yral.shared.analytics.AnalyticsProvider
import com.yral.shared.analytics.User
import com.yral.shared.analytics.events.EventData
import kotlinx.cinterop.ExperimentalForeignApi

actual class FacebookAnalyticsProvider actual constructor(
    private val eventFilter: (EventData) -> Boolean,
    private val mapConverter: (EventData) -> Map<String, Any>,
) : AnalyticsProvider {
    override val name: String = "facebook"
    override fun shouldTrackEvent(event: EventData): Boolean = eventFilter(event)

    @OptIn(ExperimentalForeignApi::class)
    override fun trackEvent(event: EventData) {
        FBSDKAppEvents.shared().logEvent(
            toValidKeyName(event.event),
            parameters = mapConverter(event) as Map<Any?, *>,
        )
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun setUserProperties(user: User) {
        FBSDKAppEvents.shared().setUserID(user.userId)
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun reset() {
        FBSDKAppEvents.shared().setUserID(null)
    }

    override fun toValidKeyName(key: String) = key
}
