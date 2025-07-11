package com.yral.shared.features.uploadvideo.analytics

import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.analytics.events.UploadVideoPageViewedEventData

class UploadVideoTelemetry(
    private val analyticsManager: AnalyticsManager,
) {
    fun uploadVideoScreenViewed() {
        analyticsManager.trackEvent(UploadVideoPageViewedEventData())
    }
}
