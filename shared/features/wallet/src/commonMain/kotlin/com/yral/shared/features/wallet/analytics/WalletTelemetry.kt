package com.yral.shared.features.wallet.analytics

import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.analytics.events.WalletPageViewedEventData

class WalletTelemetry(
    private val analyticsManager: AnalyticsManager,
) {
    fun onWalletScreenViewed() {
        analyticsManager.trackEvent(WalletPageViewedEventData())
    }
}
