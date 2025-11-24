package com.yral.shared.analytics.providers.mixpanel

import cocoapods.Mixpanel.Mixpanel
import com.yral.shared.analytics.AnalyticsProvider
import com.yral.shared.analytics.EventToMapConverter
import com.yral.shared.analytics.User
import com.yral.shared.analytics.events.EventData
import com.yral.shared.analytics.events.TokenType
import com.yral.shared.preferences.UTM_CAMPAIGN_PARAM
import com.yral.shared.preferences.UTM_CONTENT_PARAM
import com.yral.shared.preferences.UTM_MEDIUM_PARAM
import com.yral.shared.preferences.UTM_SOURCE_PARAM
import com.yral.shared.preferences.UTM_TERM_PARAM
import com.yral.shared.preferences.UtmAttributionStore
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
actual class MixpanelAnalyticsProvider actual constructor(
    private val eventFilter: (EventData) -> Boolean,
    private val mapConverter: EventToMapConverter,
    token: String,
    private val utmAttributionStore: UtmAttributionStore,
) : AnalyticsProvider {
    private companion object {
        private const val ONE_SIGNAL_PROPERTY = "\$onesignal_user_id"
    }

    override val name: String = "mixpanel"
    private val mixpanel: Mixpanel = Mixpanel.sharedInstanceWithToken(token, true)

    override fun shouldTrackEvent(event: EventData): Boolean = eventFilter(event)

    override fun trackEvent(event: EventData) {
        val props: MutableMap<String, Any?> = mapConverter.toMap(event).toMutableMap()
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

        // Attach UTM attribution as user-level properties (people + super props)
        val utmParams = utmAttributionStore.get()
        val utmParamsMap: MutableMap<String, Any?> = mutableMapOf()
        utmParams.source?.let { utmParamsMap[UTM_SOURCE_PARAM] = it }
        utmParams.medium?.let { utmParamsMap[UTM_MEDIUM_PARAM] = it }
        utmParams.campaign?.let { utmParamsMap[UTM_CAMPAIGN_PARAM] = it }
        utmParams.term?.let { utmParamsMap[UTM_TERM_PARAM] = it }
        utmParams.content?.let { utmParamsMap[UTM_CONTENT_PARAM] = it }

        mixpanel.people.set(property = ONE_SIGNAL_PROPERTY, to = user.userId)
        if (user.isLoggedIn ?: false) {
            mixpanel.identify(user.userId)
            superProps["user_id"] = user.userId
            superProps["visitor_id"] = null
        } else {
            superProps["visitor_id"] = user.userId
            superProps["user_id"] = null
        }
        mixpanel.people.set(superProps + utmParamsMap)
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
