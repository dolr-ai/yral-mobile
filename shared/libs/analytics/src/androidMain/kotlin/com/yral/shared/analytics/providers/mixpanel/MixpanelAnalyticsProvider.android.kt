package com.yral.shared.analytics.providers.mixpanel

import android.content.Context
import com.mixpanel.android.mpmetrics.MixpanelAPI
import com.mixpanel.android.sessionreplay.MPSessionReplay
import com.mixpanel.android.sessionreplay.models.MPSessionReplayConfig
import com.mixpanel.android.sessionreplay.sensitive_views.AutoMaskedView
import com.yral.shared.analytics.AnalyticsProvider
import com.yral.shared.analytics.EventToMapConverter
import com.yral.shared.analytics.User
import com.yral.shared.analytics.di.IS_DEBUG
import com.yral.shared.analytics.events.EventData
import com.yral.shared.analytics.events.TokenType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual class MixpanelAnalyticsProvider actual constructor(
    private val eventFilter: (EventData) -> Boolean,
    private val mapConverter: EventToMapConverter,
    token: String,
) : AnalyticsProvider,
    KoinComponent {
    private companion object {
        private const val ONE_SIGNAL_PROPERTY = "\$onesignal_user_id"
    }

    private val context: Context by inject()
    private val isDebug: Boolean by inject(IS_DEBUG)
    override val name: String = "mixpanel"

    private val mixpanel: MixpanelAPI =
        MixpanelAPI.getInstance(context, token, true)

    init {
        // initSessionReplay(token)
        if (isDebug) {
            mixpanel.setEnableLogging(true)
        }
    }

    private val distinctId = MutableStateFlow(mixpanel.distinctId)
    fun observeDistinctId(): Flow<String> = distinctId.asStateFlow()

    fun initSessionReplay(token: String) {
        MPSessionReplay.initialize(
            appContext = context,
            token = token,
            distinctId = mixpanel.distinctId,
            config =
                MPSessionReplayConfig(
                    wifiOnly = false,
                    recordingSessionsPercent = 100.0,
                    autoMaskedViews = setOf(AutoMaskedView.Web),
                ),
        )
    }

    override fun shouldTrackEvent(event: EventData): Boolean = eventFilter(event)

    override fun trackEvent(event: EventData) {
        val properties: MutableMap<String, Any?> = mapConverter.toMap(event).toMutableMap()
        mixpanel.trackMap(toValidKeyName(event.event), properties)
    }

    override fun setUserProperties(user: User) {
        val superProps: MutableMap<String, Any?> =
            mutableMapOf(
                "is_creator" to (user.isCreator ?: false),
                "is_logged_in" to user.isLoggedIn,
                "wallet_balance" to user.walletBalance,
                "wallet_token_type" to (user.tokenType?.serialName ?: ""),
                "canister_id" to user.canisterId,
                "is_forced_gameplay_test_user" to user.isForcedGamePlayUser,
                "is_auto_scroll_enabled" to user.isAutoScrollEnabled,
                "email_id" to user.emailId,
            )

        // Attach UTM attribution as user-level properties (people + super props)
        val utmParamsMap = user.utmParams?.toMap() ?: emptyMap()

        mixpanel.people.set(ONE_SIGNAL_PROPERTY, user.userId)
        if (user.isLoggedIn == true) {
            mixpanel.identify(user.userId)
            MPSessionReplay.getInstance()?.identify(user.userId)
            distinctId.value = mixpanel.distinctId
            superProps["user_id"] = user.userId
            superProps["visitor_id"] = null
        } else {
            superProps["visitor_id"] = user.userId
            superProps["user_id"] = null
        }
        mixpanel.people.set(JSONObject(superProps + utmParamsMap))
        mixpanel.registerSuperProperties(JSONObject(superProps))
    }

    override fun reset() {
        mixpanel.people.unset(ONE_SIGNAL_PROPERTY)
        mixpanel.reset()
        MPSessionReplay.getInstance()?.identify(mixpanel.distinctId)
        distinctId.value = mixpanel.distinctId
    }

    override fun toValidKeyName(key: String): String = key

    val TokenType.serialName: String
        get() =
            when (this) {
                TokenType.CENTS -> "cents"
                TokenType.SATS -> "sats"
                TokenType.YRAL -> "yral"
            }
}
