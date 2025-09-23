package com.yral.shared.features.profile.analytics

import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.analytics.events.CategoryName
import com.yral.shared.analytics.events.CtaType
import com.yral.shared.analytics.events.DeleteVideoInitiatedEventData
import com.yral.shared.analytics.events.GameType
import com.yral.shared.analytics.events.ProfilePageViewedEventData
import com.yral.shared.analytics.events.SourceScreen
import com.yral.shared.analytics.events.UploadVideoClickedEventData
import com.yral.shared.analytics.events.VideoClickedEventData
import com.yral.shared.analytics.events.VideoDeleteCTA
import com.yral.shared.analytics.events.VideoDeletedEventData
import com.yral.shared.analytics.events.VideoReportedEventData
import com.yral.shared.analytics.events.VideoShareClickedEventData
import com.yral.shared.data.feed.domain.FeedDetails
import com.yral.shared.reportVideo.domain.models.VideoReportReason

class ProfileTelemetry(
    private val analyticsManager: AnalyticsManager,
) {
    fun onProfileScreenViewed(
        totalVideos: Int,
        publisherUserId: String,
    ) {
        analyticsManager.trackEvent(
            event =
                ProfilePageViewedEventData(
                    totalVideos = totalVideos,
                    isOwnProfile = true,
                    publisherUserId = publisherUserId,
                ),
        )
    }

    fun onUploadVideoClicked() {
        analyticsManager.trackEvent(UploadVideoClickedEventData())
    }

    fun onVideoClicked(feedDetails: FeedDetails) {
        analyticsManager.trackEvent(
            event =
                VideoClickedEventData(
                    videoId = feedDetails.videoID,
                    publisherUserId = feedDetails.principalID,
                    likeCount = feedDetails.likeCount.toLong(),
                    viewCount = feedDetails.viewCount.toLong(),
                    isNsfw = feedDetails.isNSFW(),
                    ctaType = CtaType.DELETE,
                    shareCount = 0,
                    isGameEnabled = true,
                    gameType = GameType.SMILEY,
                    pageName = CategoryName.PROFILE,
                ),
        )
    }

    fun onDeleteInitiated(feedDetails: FeedDetails) {
        analyticsManager.trackEvent(
            event =
                DeleteVideoInitiatedEventData(
                    pageName = CategoryName.PROFILE,
                    videoId = feedDetails.videoID,
                ),
        )
    }

    fun onDeleted(
        feedDetails: FeedDetails,
        catType: VideoDeleteCTA,
    ) {
        analyticsManager.trackEvent(
            event =
                VideoDeletedEventData(
                    pageName = CategoryName.PROFILE,
                    videoId = feedDetails.videoID,
                    ctaType = catType,
                ),
        )
    }

    fun onShareClicked(
        feedDetails: FeedDetails,
        userPrincipalId: String?,
    ) {
        analyticsManager.trackEvent(
            VideoShareClickedEventData(
                feedDetails.videoID,
                SourceScreen.PROFILE,
                feedDetails.principalID == userPrincipalId,
            ),
        )
    }

    fun videoClicked(
        feedDetails: FeedDetails,
        ctaType: CtaType,
    ) {
        analyticsManager.trackEvent(
            event =
                VideoClickedEventData(
                    videoId = feedDetails.videoID,
                    publisherUserId = feedDetails.principalID,
                    likeCount = feedDetails.likeCount.toLong(),
                    viewCount = feedDetails.viewCount.toLong(),
                    isNsfw = feedDetails.isNSFW(),
                    ctaType = ctaType,
                    shareCount = 0,
                    isGameEnabled = true,
                    gameType = GameType.SMILEY,
                    pageName = CategoryName.PROFILE,
                ),
        )
    }

    fun videoReportedSuccessfully(
        feedDetails: FeedDetails,
        reason: VideoReportReason,
    ) {
        analyticsManager.trackEvent(
            event =
                VideoReportedEventData(
                    videoId = feedDetails.videoID,
                    publisherUserId = feedDetails.principalID,
                    isNsfw = feedDetails.isNSFW(),
                    isGameEnabled = true,
                    gameType = GameType.SMILEY,
                    reason = reason.reason,
                ),
        )
    }
}
