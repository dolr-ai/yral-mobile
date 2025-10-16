package com.yral.shared.analytics.providers.mixpanel

import cocoapods.Mixpanel.Mixpanel
import com.yral.shared.analytics.AnalyticsProvider
import com.yral.shared.analytics.EventToMapConverter
import com.yral.shared.analytics.User
import com.yral.shared.analytics.events.EventData
import com.yral.shared.analytics.events.TokenType
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
actual class MixpanelAnalyticsProvider actual constructor(
    private val eventFilter: (EventData) -> Boolean,
    private val mapConverter: EventToMapConverter,
    token: String,
) : AnalyticsProvider {
    private companion object {
        private const val ONE_SIGNAL_PROPERTY = "\$onesignal_user_id"
    }

    override val name: String = "mixpanel"
    private val mixpanel: Mixpanel = Mixpanel.sharedInstanceWithToken(token, true)

    override fun shouldTrackEvent(event: EventData): Boolean = eventFilter(event)

    override fun trackEvent(event: EventData) {
        val props: Map<String, Any> = mapConverter.toMap(event)
        mixpanel.track(
            event = toValidKeyName(event.event),
            properties = props.mapValues { it.value as Any? },
        )
    }

    override fun setUserProperties(user: User) {
        val isCreatorCurrentStatus = mixpanel.currentSuperProperties()["is_creator"] as? Boolean
        val isCreator = user.isCreator ?: isCreatorCurrentStatus
        val superProps: MutableMap<Any?, Any?> =
            mutableMapOf(
                "is_creator" to (isCreator ?: false),
                "is_logged_in" to user.isLoggedIn,
                "wallet_balance" to user.walletBalance,
                "token_type" to (user.tokenType?.serialName ?: ""),
                "canister_id" to user.canisterId,
                "is_Forced Gameplay_test_user" to user.isForcedGamePlayUser,
                "email_id" to user.emailId,
            )
        mixpanel.people.set(property = ONE_SIGNAL_PROPERTY, to = user.userId)
        if (user.isLoggedIn ?: false) {
            mixpanel.identify(user.userId)
            superProps["user_id"] = user.userId
            superProps["visitor_id"] = null
        } else {
            superProps["visitor_id"] = user.userId
            superProps["user_id"] = null
        }
        mixpanel.registerSuperProperties(superProps)
    }

    override fun reset() {
        mixpanel.people.unset(properties = listOf(ONE_SIGNAL_PROPERTY))
        mixpanel.reset()
    }

    override fun toValidKeyName(key: String) = key

    val TokenType.serialName: String
        get() =
            when (this) {
                TokenType.CENTS -> "cents"
                TokenType.SATS -> "sats"
                TokenType.YRAL -> "yral"
            }
}
