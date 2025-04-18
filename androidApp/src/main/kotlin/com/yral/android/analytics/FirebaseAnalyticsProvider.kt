package com.yral.android.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.yral.shared.analytics.core.AnalyticsProvider
import com.yral.shared.analytics.core.Event
import com.yral.shared.analytics.core.User

class FirebaseAnalyticsProvider(
    private val context: Context,
    private val eventFilter: (Event) -> Boolean = { true },
) : AnalyticsProvider {
    private val firebaseAnalytics: FirebaseAnalytics by lazy {
        FirebaseAnalytics.getInstance(context)
    }

    override val name: String = "firebase"

    override fun shouldTrackEvent(event: Event): Boolean = eventFilter(event)

    override fun trackEvent(event: Event) {
        val bundle = Bundle()
        event.properties.forEach { (key, value) ->
            when (value) {
                is String -> bundle.putString(key, value)
                is Int -> bundle.putInt(key, value)
                is Long -> bundle.putLong(key, value)
                is Float -> bundle.putFloat(key, value)
                is Double -> bundle.putDouble(key, value)
                is Boolean -> bundle.putBoolean(key, value)
                else -> bundle.putString(key, value.toString())
            }
        }
        firebaseAnalytics.logEvent(event.name, bundle)
    }

    override fun flush() {
        // Firebase Analytics automatically batches and sends events
    }

    override fun setUserProperties(user: User) {
        firebaseAnalytics.setUserId(user.userId)
        firebaseAnalytics.setUserProperty("name", user.name)
        firebaseAnalytics.setUserProperty("emailId", user.emailId)
    }

    override fun reset() {
        firebaseAnalytics.resetAnalyticsData()
        firebaseAnalytics.setUserId("")
        firebaseAnalytics.setUserProperty("name", "")
        firebaseAnalytics.setUserProperty("emailId", "")
    }
}
