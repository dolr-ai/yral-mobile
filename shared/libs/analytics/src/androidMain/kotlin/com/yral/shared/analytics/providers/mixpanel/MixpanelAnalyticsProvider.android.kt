package com.yral.shared.analytics.providers.mixpanel

import android.content.Context
import com.mixpanel.android.mpmetrics.MixpanelAPI
import com.mixpanel.android.sessionreplay.MPSessionReplay
import com.mixpanel.android.sessionreplay.models.MPSessionReplayConfig
import com.mixpanel.android.sessionreplay.sensitive_views.AutoMaskedView
import com.yral.shared.analytics.AnalyticsProvider
import com.yral.shared.analytics.EventToMapConverter
import com.yral.shared.analytics.SuperProperties
import com.yral.shared.analytics.User
import com.yral.shared.analytics.di.IS_DEBUG
import com.yral.shared.analytics.events.EventData
import com.yral.shared.analytics.toSuperProperties
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
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
        val properties = mapConverter.toMap(event)
        mixpanel.trackMap(toValidKeyName(event.event), properties)
    }

    override fun setUserProperties(user: User) {
        mixpanel.people.set(ONE_SIGNAL_PROPERTY, user.userId)
        if (user.isLoggedIn == true) {
            mixpanel.identify(user.userId)
            MPSessionReplay.getInstance()?.identify(user.userId)
            distinctId.value = mixpanel.distinctId
        }
        registerSuperProperties(user.toSuperProperties())
    }

    override fun reset(resetOnlyProperties: Boolean) {
        mixpanel.people.unset(ONE_SIGNAL_PROPERTY)
        if (resetOnlyProperties) {
            registerSuperProperties(SuperProperties())
        } else {
            mixpanel.reset()
            MPSessionReplay.getInstance()?.identify(mixpanel.distinctId)
            distinctId.value = mixpanel.distinctId
        }
    }

    private fun registerSuperProperties(properties: SuperProperties) {
        val superProperties = Json.encodeToString(properties)
        mixpanel.registerSuperProperties(JSONObject(superProperties))
    }

    override fun toValidKeyName(key: String): String = key
}
