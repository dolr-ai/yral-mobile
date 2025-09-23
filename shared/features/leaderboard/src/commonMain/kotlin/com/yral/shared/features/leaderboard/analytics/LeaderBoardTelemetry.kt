package com.yral.shared.features.leaderboard.analytics

import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.analytics.events.LeaderBoardCalendarClickedEventData
import com.yral.shared.analytics.events.LeaderBoardDaySelectedEventData
import com.yral.shared.analytics.events.LeaderBoardPageLoadedEventData
import com.yral.shared.analytics.events.LeaderBoardPageViewedEventData
import com.yral.shared.analytics.events.LeaderBoardTabClickedEventData
import com.yral.shared.analytics.events.LeaderBoardTabType
import com.yral.shared.features.leaderboard.data.models.LeaderboardMode

class LeaderBoardTelemetry(
    private val analyticsManager: AnalyticsManager,
) {
    fun leaderboardPageViewed(tab: LeaderboardMode) {
        analyticsManager.trackEvent(LeaderBoardPageViewedEventData(tab.toAnalyticsTab()))
    }

    fun leaderboardPageLoaded(
        tab: LeaderboardMode,
        rank: Int,
        visibleRows: Int,
    ) {
        analyticsManager.trackEvent(
            LeaderBoardPageLoadedEventData(
                leaderBoardTabType = tab.toAnalyticsTab(),
                rank = rank,
                visibleRows = visibleRows,
            ),
        )
    }

    fun leaderboardTabClicked(tab: LeaderboardMode) {
        analyticsManager.trackEvent(LeaderBoardTabClickedEventData(tab.toAnalyticsTab()))
    }

    fun leaderboardCalendarClicked(
        tab: LeaderboardMode,
        rank: Int,
    ) {
        analyticsManager.trackEvent(
            LeaderBoardCalendarClickedEventData(
                leaderBoardTabType = tab.toAnalyticsTab(),
                rank = rank,
            ),
        )
    }

    fun leaderboardDaySelected(
        day: Int,
        date: String,
        rank: Int,
        visibleRows: Int,
    ) {
        analyticsManager.trackEvent(
            LeaderBoardDaySelectedEventData(
                day = day,
                date = date,
                rank = rank,
                visibleRows = visibleRows,
            ),
        )
    }

    private fun LeaderboardMode.toAnalyticsTab(): LeaderBoardTabType =
        when (this) {
            LeaderboardMode.DAILY -> LeaderBoardTabType.DAILY
            LeaderboardMode.ALL_TIME -> LeaderBoardTabType.ALL
        }
}
