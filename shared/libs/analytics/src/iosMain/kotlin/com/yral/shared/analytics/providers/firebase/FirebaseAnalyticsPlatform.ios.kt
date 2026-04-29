@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.yral.shared.analytics.providers.firebase

import cocoapods.FirebaseAnalytics.FIRAnalytics

internal actual fun createFirebaseAnalyticsPlatform(): FirebaseAnalyticsPlatform = IosFirebaseAnalyticsPlatform

private object IosFirebaseAnalyticsPlatform : FirebaseAnalyticsPlatform {
    override fun logEvent(
        name: String,
        parameters: Map<String, Any>,
    ) {
        FIRAnalytics.logEventWithName(
            name = name,
            parameters = parameters.toFirebaseParameters(),
        )
    }

    override fun setUserId(userId: String) {
        FIRAnalytics.setUserID(userId)
    }

    override fun setUserProperty(
        key: String,
        value: String,
    ) {
        FIRAnalytics.setUserPropertyString(value, forName = key)
    }

    override fun resetAnalyticsData() {
        FIRAnalytics.resetAnalyticsData()
    }
}

private fun Map<String, Any>.toFirebaseParameters(): Map<Any?, Any> = mapKeys { (key, _) -> key }
