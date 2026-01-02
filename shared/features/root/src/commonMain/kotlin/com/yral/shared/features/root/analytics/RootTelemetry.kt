package com.yral.shared.features.root.analytics

import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.analytics.User
import com.yral.shared.analytics.events.BottomNavigationClickedEventData
import com.yral.shared.analytics.events.CategoryName
import com.yral.shared.analytics.events.FirstAppLaunchEventData
import com.yral.shared.analytics.events.SplashScreenViewedEventData
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class RootTelemetry(
    private val analyticsManager: AnalyticsManager,
) {
    @OptIn(ExperimentalTime::class)
    fun onFirstAppLaunch(now: Instant) {
        analyticsManager.trackEvent(FirstAppLaunchEventData(date = now.toString()))
        analyticsManager.flush()
    }

    fun onSplashScreenViewed() {
        analyticsManager.trackEvent(SplashScreenViewedEventData())
    }

    fun bottomNavigationClicked(categoryName: CategoryName) {
        analyticsManager.trackEvent(BottomNavigationClickedEventData(categoryName))
    }

    fun setUser(user: User?) {
        user?.let { analyticsManager.setUserProperties(user) }
    }
}
