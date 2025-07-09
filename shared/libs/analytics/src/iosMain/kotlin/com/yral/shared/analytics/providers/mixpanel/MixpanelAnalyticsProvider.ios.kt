package com.yral.shared.analytics.providers.mixpanel

import cocoapods.Mixpanel.Mixpanel
import com.yral.shared.analytics.AnalyticsProvider
import com.yral.shared.analytics.User
import com.yral.shared.analytics.events.EventData
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
actual class MixpanelAnalyticsProvider actual constructor(
    private val eventFilter: (EventData) -> Boolean,
    private val mapConverter: (EventData) -> Map<String, Any>,
    token: String,
) : AnalyticsProvider {
    override val name: String = "mixpanel"
    private val mixpanel: Mixpanel = Mixpanel.sharedInstanceWithToken(token, true)

    override fun shouldTrackEvent(event: EventData): Boolean = eventFilter(event)

    override fun trackEvent(event: EventData) {
        mixpanel.track(toValidKeyName(event.event), mapConverter(event) as Map<Any?, *>)
    }

    override fun setUserProperties(user: User) {
        mixpanel.identify(user.userId)
        val isCreatorCurrentStatus = mixpanel.currentSuperProperties().get("is_creator") as? Boolean
        val isCreator = user.isCreator ?: isCreatorCurrentStatus
        val superProps: MutableMap<Any?, Any?> =
            mutableMapOf(
                "is_creator" to (isCreator ?: false),
                "is_logged_in" to user.isLoggedIn,
                "sats_balance" to user.satsBalance,
            )
        if (user.isLoggedIn) {
            superProps["user_id"] = user.userId
            superProps["visitor_id"] = null
        } else {
            superProps["visitor_id"] = user.userId
            superProps["user_id"] = null
        }
        mixpanel.registerSuperProperties(superProps)
    }

    override fun reset() {
        mixpanel.reset()
    }

    override fun toValidKeyName(key: String) = key
}
