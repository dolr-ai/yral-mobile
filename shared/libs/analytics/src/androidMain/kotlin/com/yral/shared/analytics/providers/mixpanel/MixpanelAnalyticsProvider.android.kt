package com.yral.shared.analytics.providers.mixpanel

import com.mixpanel.android.mpmetrics.MixpanelAPI
import com.yral.shared.analytics.AnalyticsProvider
import com.yral.shared.analytics.EventToMapConverter
import com.yral.shared.analytics.User
import com.yral.shared.analytics.events.EventData
import com.yral.shared.core.platform.PlatformResources
import org.json.JSONObject

actual class MixpanelAnalyticsProvider actual constructor(
    platformResources: PlatformResources,
    private val eventFilter: (EventData) -> Boolean,
    private val mapConverter: EventToMapConverter,
    token: String,
) : AnalyticsProvider {
    override val name: String = "mixpanel"

    private val mixpanel: MixpanelAPI =
        MixpanelAPI.getInstance(platformResources.activityContext, token, true)

    override fun shouldTrackEvent(event: EventData): Boolean = eventFilter(event)

    override fun trackEvent(event: EventData) {
        val properties = mapConverter.toMap(event)
        mixpanel.trackMap(toValidKeyName(event.event), properties)
    }

    override fun setUserProperties(user: User) {
        val superProps: MutableMap<Any, Any?> =
            mutableMapOf(
                "is_creator" to (user.isCreator ?: false),
                "is_logged_in" to user.isLoggedIn,
                "sats_balance" to user.satsBalance,
                "canister_id" to user.canisterId,
            )
        if (user.isLoggedIn == true) {
            mixpanel.identify(user.userId)
            superProps["user_id"] = user.userId
            superProps["visitor_id"] = null
        } else {
            superProps["visitor_id"] = user.userId
            superProps["user_id"] = null
        }
        mixpanel.registerSuperProperties(JSONObject(superProps))
    }

    override fun reset() {
        mixpanel.reset()
    }

    override fun toValidKeyName(key: String): String = key
}
