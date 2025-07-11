package com.yral.shared.features.account.analytics

import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.analytics.events.MenuPageViewedEventData

class AccountsTelemetry(
    private val analyticsManager: AnalyticsManager,
) {
    fun onMenuScreenViewed() {
        analyticsManager.trackEvent(MenuPageViewedEventData())
    }
}
