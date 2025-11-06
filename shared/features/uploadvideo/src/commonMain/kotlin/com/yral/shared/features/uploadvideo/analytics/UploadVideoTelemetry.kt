package com.yral.shared.features.uploadvideo.analytics

import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.analytics.events.AiVideoGenFailureType
import com.yral.shared.analytics.events.AiVideoGeneratedData
import com.yral.shared.analytics.events.CreateAIVideoClickedData
import com.yral.shared.analytics.events.FileSelectionSuccessEventData
import com.yral.shared.analytics.events.GameType
import com.yral.shared.analytics.events.SelectFileClickedEventData
import com.yral.shared.analytics.events.UploadVideoPageViewedEventData
import com.yral.shared.analytics.events.VideoCreationPageViewedEventData
import com.yral.shared.analytics.events.VideoCreationType
import com.yral.shared.analytics.events.VideoGenerationModelSelectedData
import com.yral.shared.analytics.events.VideoUploadErrorShownEventData
import com.yral.shared.analytics.events.VideoUploadInitiatedEventData
import com.yral.shared.analytics.events.VideoUploadSuccessEventData
import com.yral.shared.analytics.events.VideoUploadTypeSelectedData
import com.yral.shared.core.analytics.AffiliateAttributionStore
import com.yral.shared.core.session.SessionManager

class UploadVideoTelemetry(
    private val analyticsManager: AnalyticsManager,
    private val sessionManager: SessionManager,
    private val affiliateAttributionStore: AffiliateAttributionStore,
) {
    fun uploadVideoScreenViewed() {
        analyticsManager.trackEvent(UploadVideoPageViewedEventData())
    }

    fun videoCreationPageViewed(
        type: VideoCreationType,
        creditsFetched: Boolean? = null,
        creditsAvailable: Int? = null,
    ) {
        analyticsManager.trackEvent(
            event =
                VideoCreationPageViewedEventData(
                    type = type,
                    creditsFetched = creditsFetched,
                    creditsAvailable = creditsAvailable,
                ),
        )
    }

    fun selectFile() {
        analyticsManager.trackEvent(SelectFileClickedEventData())
    }

    fun fileSelected() {
        analyticsManager.trackEvent(FileSelectionSuccessEventData("video"))
    }

    fun uploadInitiated(type: VideoCreationType) {
        analyticsManager.trackEvent(
            event =
                VideoUploadInitiatedEventData(
                    captionAdded = true,
                    hashtagsAdded = true,
                    type = type,
                    affiliate = affiliateAttributionStore.peek(),
                ),
        )
    }

    fun uploadSuccess(
        videoId: String,
        type: VideoCreationType,
    ) {
        analyticsManager.trackEvent(
            event =
                VideoUploadSuccessEventData(
                    videoId = videoId,
                    publisherUserId = sessionManager.userPrincipal ?: "",
                    isNsfw = false,
                    gameType = GameType.SMILEY,
                    isGameEnabled = true,
                    type = type,
                    affiliate = affiliateAttributionStore.peek(),
                ),
        )
    }

    fun uploadFailed(
        reason: String,
        type: VideoCreationType,
    ) {
        analyticsManager.trackEvent(
            VideoUploadErrorShownEventData(
                reason,
                type,
                affiliate = affiliateAttributionStore.peek(),
            ),
        )
    }

    fun videoUploadTypeSelected(type: VideoCreationType) {
        analyticsManager.trackEvent(VideoUploadTypeSelectedData(type))
    }

    fun videoGenerationModelSelected(model: String) {
        analyticsManager.trackEvent(VideoGenerationModelSelectedData(model))
    }

    fun createAiVideoClicked(
        model: String,
        prompt: String,
    ) {
        analyticsManager.trackEvent(CreateAIVideoClickedData(model, prompt))
    }

    fun aiVideoGenerated(
        model: String,
        prompt: String,
        isSuccess: Boolean,
        reason: String?,
        reasonType: AiVideoGenFailureType?,
    ) {
        analyticsManager.trackEvent(
            AiVideoGeneratedData(
                model,
                prompt,
                isSuccess,
                reason,
                reasonType,
            ),
        )
    }
}
