package com.yral.shared.features.profile.analytics

import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.analytics.events.ProfilePageViewedEventData

class ProfileTelemetry(
    private val analyticsManager: AnalyticsManager,
) {
    fun onProfileScreenViewed(
        totalVideos: Int,
        publisherUserId: String,
    ) {
        analyticsManager.trackEvent(
            ProfilePageViewedEventData(
                totalVideos = totalVideos,
                isOwnProfile = totalVideos > 0,
                publisherUserId = publisherUserId,
            ),
        )
    }
}
