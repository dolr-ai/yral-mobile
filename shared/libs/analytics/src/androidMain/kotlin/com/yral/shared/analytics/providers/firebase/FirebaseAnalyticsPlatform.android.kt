package com.yral.shared.analytics.providers.firebase

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.yral.shared.analytics.toBundle
import com.yral.shared.koin.koinInstance

internal actual fun createFirebaseAnalyticsPlatform(): FirebaseAnalyticsPlatform =
    AndroidFirebaseAnalyticsPlatform(
        analytics = FirebaseAnalytics.getInstance(koinInstance.get<Context>().applicationContext),
    )

private class AndroidFirebaseAnalyticsPlatform(
    private val analytics: FirebaseAnalytics,
) : FirebaseAnalyticsPlatform {
    override fun logEvent(
        name: String,
        parameters: Map<String, Any>,
    ) {
        analytics.logEvent(name, toBundle(parameters))
    }

    override fun setUserId(userId: String) {
        analytics.setUserId(userId)
    }

    override fun setUserProperty(
        key: String,
        value: String,
    ) {
        analytics.setUserProperty(key, value)
    }

    override fun resetAnalyticsData() {
        analytics.resetAnalyticsData()
    }
}
