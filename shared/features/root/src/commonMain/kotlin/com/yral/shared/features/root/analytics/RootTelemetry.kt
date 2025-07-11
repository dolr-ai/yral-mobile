package com.yral.shared.features.root.analytics

import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.analytics.events.BottomNavigationClickedEventData
import com.yral.shared.analytics.events.CategoryName
import com.yral.shared.analytics.events.SplashScreenViewedEventData

class RootTelemetry(
    private val analyticsManager: AnalyticsManager,
) {
    fun onSplashScreenViewed() {
        analyticsManager.trackEvent(SplashScreenViewedEventData())
    }

    fun bottomNavigationClicked(categoryName: CategoryName) {
        analyticsManager.trackEvent(BottomNavigationClickedEventData(categoryName))
    }
}
