package com.yral.shared.analytics.providers.snowplow

import android.content.Context
import com.snowplowanalytics.snowplow.Snowplow
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration
import com.snowplowanalytics.snowplow.event.Structured
import com.snowplowanalytics.snowplow.network.HttpMethod
import com.snowplowanalytics.snowplow.tracker.DevicePlatform
import com.snowplowanalytics.snowplow.tracker.LogLevel
import com.yral.shared.analytics.AnalyticsProvider
import com.yral.shared.analytics.EventToMapConverter
import com.yral.shared.analytics.User
import com.yral.shared.analytics.events.EventData
import com.yral.shared.core.AppConfigurations
import org.json.JSONObject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual class SnowplowAnalyticsProvider actual constructor(
    private val eventFilter: (EventData) -> Boolean,
    private val mapConverter: EventToMapConverter,
    private val appId: String,
) : AnalyticsProvider,
    KoinComponent {
    private val context: Context by inject()
    override val name: String = "snowplow"

    private val tracker =
        Snowplow.createTracker(
            context,
            "yral-mobile",
            NetworkConfiguration(
                "https://${AppConfigurations.SNOWPLOW_COLLECTOR_URL}",
                HttpMethod.POST,
            ),
            TrackerConfiguration(appId)
                .devicePlatform(DevicePlatform.Mobile)
                .base64encoding(false)
                .logLevel(LogLevel.OFF)
                .sessionContext(true)
                .platformContext(true)
                .applicationContext(true)
                .lifecycleAutotracking(true)
                .screenViewAutotracking(true)
                .screenEngagementAutotracking(true)
                .installAutotracking(true)
                .exceptionAutotracking(true)
                .diagnosticAutotracking(false),
        )

    override fun shouldTrackEvent(event: EventData): Boolean = eventFilter(event)

    override fun trackEvent(event: EventData) {
        val properties = mapConverter.toMap(event)
        val propertyJson = JSONObject(properties.mapValues { it.value.toString() }).toString()
        // Snowplow atomic event schema limits se_property to maxLength=1000.
        // Truncate to prevent atomic_field_length_exceeded bad rows.
        // The full event data is preserved in the raw topic (snowplow-raw) for reprocessing.
        val truncatedProperty =
            if (propertyJson.length <= MAX_SE_PROPERTY_LENGTH) {
                propertyJson
            } else {
                propertyJson.substring(0, MAX_SE_PROPERTY_LENGTH - TRUNCATION_SUFFIX.length) + TRUNCATION_SUFFIX
            }
        val snowplowEvent =
            Structured(
                category = event.featureName,
                action = event.event,
            ).property(truncatedProperty)
        tracker.track(snowplowEvent)
    }

    override fun setUserProperties(user: User) {
        tracker.subject.userId = user.userId
    }

    override fun reset() {
        tracker.subject.userId = null
    }

    override fun toValidKeyName(key: String): String = key

    private companion object {
        // Snowplow atomic event schema: se_property maxLength = 1000
        const val MAX_SE_PROPERTY_LENGTH = 1000
        const val TRUNCATION_SUFFIX = "..."
    }
}
