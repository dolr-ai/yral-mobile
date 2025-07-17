package com.yral.shared.features.game.analytics

import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.analytics.events.LeaderBoardPageViewedEventData

class LeaderBoardTelemetry(
    private val analyticsManager: AnalyticsManager,
) {
    fun leaderboardPageViewed() {
        analyticsManager.trackEvent(LeaderBoardPageViewedEventData())
    }
}
