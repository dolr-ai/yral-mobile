package com.yral.shared.analytics.providers.snowplow

import cocoapods.SnowplowTracker.SPDevicePlatformMobile
import cocoapods.SnowplowTracker.SPHttpMethodPost
import cocoapods.SnowplowTracker.SPLogLevelOff
import cocoapods.SnowplowTracker.SPNetworkConfiguration
import cocoapods.SnowplowTracker.SPSnowplow
import cocoapods.SnowplowTracker.SPStructured
import cocoapods.SnowplowTracker.SPTrackerConfiguration
import com.yral.shared.analytics.AnalyticsProvider
import com.yral.shared.analytics.EventToMapConverter
import com.yral.shared.analytics.User
import com.yral.shared.analytics.events.EventData
import com.yral.shared.core.AppConfigurations
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
actual class SnowplowAnalyticsProvider actual constructor(
    private val eventFilter: (EventData) -> Boolean,
    private val mapConverter: EventToMapConverter,
    private val appId: String,
) : AnalyticsProvider {
    override val name: String = "snowplow"

    private val tracker =
        SPSnowplow.createTrackerWithNamespace(
            "yral-mobile",
            network =
                SPNetworkConfiguration(
                    "https://${AppConfigurations.SNOWPLOW_COLLECTOR_URL}",
                    method = SPHttpMethodPost,
                ),
            configurations =
                listOf(
                    SPTrackerConfiguration().apply {
                        setAppId(appId)
                        setDevicePlatform(SPDevicePlatformMobile)
                        setBase64Encoding(false)
                        setLogLevel(SPLogLevelOff)
                        setSessionContext(true)
                        setPlatformContext(true)
                        setApplicationContext(true)
                        setLifecycleAutotracking(true)
                        setScreenViewAutotracking(true)
                        setScreenEngagementAutotracking(true)
                        setInstallAutotracking(true)
                        setExceptionAutotracking(true)
                        setDiagnosticAutotracking(false)
                    },
                ),
        )

    override fun shouldTrackEvent(event: EventData): Boolean = eventFilter(event)

    override fun trackEvent(event: EventData) {
        val properties = mapConverter.toMap(event)
        val propertyJson = buildPropertyJson(properties)
        val snowplowEvent =
            SPStructured(
                category = event.featureName,
                action = event.event,
            ).property(propertyJson)
        tracker?.track(snowplowEvent)
    }

    override fun setUserProperties(user: User) {
        tracker?.subject()?.setUserId(user.userId)
    }

    override fun reset() {
        tracker?.subject()?.setUserId(null)
    }

    override fun toValidKeyName(key: String): String = key

    private fun buildPropertyJson(properties: Map<String, Any?>): String =
        properties.entries.joinToString(separator = ",", prefix = "{", postfix = "}") { (key, value) ->
            val k = key.replace("\\", "\\\\").replace("\"", "\\\"")
            val v = (value?.toString() ?: "").replace("\\", "\\\\").replace("\"", "\\\"")
            "\"$k\":\"$v\""
        }
}
