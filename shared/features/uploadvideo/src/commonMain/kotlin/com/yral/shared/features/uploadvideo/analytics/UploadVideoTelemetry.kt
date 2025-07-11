package com.yral.shared.features.uploadvideo.analytics

import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.analytics.events.FileSelectionSuccessEventData
import com.yral.shared.analytics.events.GameType
import com.yral.shared.analytics.events.SelectFileClickedEventData
import com.yral.shared.analytics.events.UploadVideoPageViewedEventData
import com.yral.shared.analytics.events.VideoUploadErrorShownEventData
import com.yral.shared.analytics.events.VideoUploadInitiatedEventData
import com.yral.shared.analytics.events.VideoUploadSuccessEventData
import com.yral.shared.core.session.SessionManager

class UploadVideoTelemetry(
    private val analyticsManager: AnalyticsManager,
    private val sessionManager: SessionManager,
) {
    fun uploadVideoScreenViewed() {
        analyticsManager.trackEvent(UploadVideoPageViewedEventData())
    }

    fun selectFile() {
        analyticsManager.trackEvent(SelectFileClickedEventData())
    }

    fun fileSelected() {
        analyticsManager.trackEvent(FileSelectionSuccessEventData("video"))
    }

    fun uploadInitiated() {
        analyticsManager.trackEvent(
            event =
                VideoUploadInitiatedEventData(
                    captionAdded = true,
                    hashtagsAdded = true,
                ),
        )
    }

    fun uploadSuccess(videoId: String) {
        analyticsManager.trackEvent(
            event =
                VideoUploadSuccessEventData(
                    videoId = videoId,
                    publisherUserId = sessionManager.getUserPrincipal() ?: "",
                    isNsfw = false,
                    gameType = GameType.SMILEY,
                    isGameEnabled = true,
                ),
        )
    }

    fun uploadFailed(reason: String) {
        analyticsManager.trackEvent(VideoUploadErrorShownEventData(reason))
    }
}
