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

        val superProps: Map<Any?, Any> =
            mapOf(
                "user_id" to user.userId,
                "visitor_id" to mixpanel.distinctId,
            )
        mixpanel.registerSuperProperties(superProps)
        val peopleProps: Map<Any?, Any> =
            mapOf(
                "user_id" to user.userId,
                "canister_id" to user.canisterId,
                "user_type" to user.userType.name.lowercase(),
                "token_wallet_balance" to user.tokenWalletBalance,
                "token_type" to user.tokenType.name.lowercase(),
            )

        peopleProps.forEach { (k, v) ->
            mixpanel.people.set(k as String, v)
        }
    }

    override fun reset() {
        mixpanel.reset()
    }

    override fun toValidKeyName(key: String) = key
}
