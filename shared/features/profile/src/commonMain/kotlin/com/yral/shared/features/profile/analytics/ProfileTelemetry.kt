package com.yral.shared.features.profile.analytics

import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.analytics.constants.Features
import com.yral.shared.analytics.events.CategoryName
import com.yral.shared.analytics.events.CtaType
import com.yral.shared.analytics.events.DeleteVideoInitiatedEventData
import com.yral.shared.analytics.events.EditProfileCancelledEventData
import com.yral.shared.analytics.events.EditProfileCompletedEventData
import com.yral.shared.analytics.events.EditProfileSource
import com.yral.shared.analytics.events.EditProfileStartedEventData
import com.yral.shared.analytics.events.FollowersListTab
import com.yral.shared.analytics.events.FollowersListViewedEventData
import com.yral.shared.analytics.events.GameType
import com.yral.shared.analytics.events.ProfilePageViewedEventData
import com.yral.shared.analytics.events.SourceScreen
import com.yral.shared.analytics.events.UploadVideoClickedEventData
import com.yral.shared.analytics.events.UserFollowedEventData
import com.yral.shared.analytics.events.UserUnFollowedEventData
import com.yral.shared.analytics.events.VideoClickedEventData
import com.yral.shared.analytics.events.VideoDeleteCTA
import com.yral.shared.analytics.events.VideoDeletedEventData
import com.yral.shared.analytics.events.VideoDownloadedEventData
import com.yral.shared.analytics.events.VideoReportedEventData
import com.yral.shared.analytics.events.VideoShareClickedEventData
import com.yral.shared.analytics.events.VideoStartedEventData
import com.yral.shared.data.domain.models.FeedDetails
import com.yral.shared.reportVideo.domain.models.VideoReportReason

class ProfileTelemetry(
    private val analyticsManager: AnalyticsManager,
) {
    private val trackedStarted = mutableSetOf<String>()

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

    fun onEditProfileStarted(source: EditProfileSource) {
        analyticsManager.trackEvent(EditProfileStartedEventData(source))
    }

    fun onEditProfileCompleted(
        usernameUpdated: Boolean,
        profileImageUpdated: Boolean,
        bioUpdated: Boolean,
    ) {
        analyticsManager.trackEvent(
            EditProfileCompletedEventData(
                usernameUpdated = usernameUpdated,
                profileImageUpdated = profileImageUpdated,
                bioUpdated = bioUpdated,
            ),
        )
    }

    fun onEditProfileCancelled() {
        analyticsManager.trackEvent(EditProfileCancelledEventData())
    }

    fun onVideoClicked(feedDetails: FeedDetails) {
        analyticsManager.trackEvent(
            event =
                VideoClickedEventData(
                    featureName = Features.PROFILE.getFeatureName(),
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
                    featureName = Features.PROFILE.getFeatureName(),
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
                    featureName = Features.PROFILE.getFeatureName(),
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
                featureName = Features.PROFILE.getFeatureName(),
                videoId = feedDetails.videoID,
                sourceScreen = SourceScreen.PROFILE,
                isOwner = feedDetails.principalID == userPrincipalId,
            ),
        )
    }

    fun trackVideoStarted(feedDetails: FeedDetails) {
        if (trackedStarted.contains(feedDetails.videoID)) return
        trackedStarted += feedDetails.videoID
        analyticsManager.trackEvent(
            event =
                VideoStartedEventData(
                    featureName = Features.PROFILE.getFeatureName(),
                    videoId = feedDetails.videoID,
                    publisherUserId = feedDetails.principalID,
                    likeCount = feedDetails.likeCount.toLong(),
                    viewCount = feedDetails.viewCount.toLong(),
                    isNsfw = feedDetails.isNSFW(),
                    shareCount = 0,
                    isGameEnabled = true,
                    gameType = GameType.SMILEY,
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
                    featureName = Features.PROFILE.getFeatureName(),
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
                    featureName = Features.PROFILE.getFeatureName(),
                    videoId = feedDetails.videoID,
                    publisherUserId = feedDetails.principalID,
                    isNsfw = feedDetails.isNSFW(),
                    isGameEnabled = true,
                    gameType = GameType.SMILEY,
                    reason = reason.reason,
                ),
        )
    }

    fun followClicked(publisherUserId: String) {
        analyticsManager.trackEvent(
            UserFollowedEventData(
                featureName = Features.PROFILE.getFeatureName(),
                publisherUserId = publisherUserId,
                source = SourceScreen.PROFILE,
                ctaType = CtaType.FOLLOW,
            ),
        )
    }

    fun unFollowClicked(publisherUserId: String) {
        analyticsManager.trackEvent(
            UserUnFollowedEventData(
                featureName = Features.PROFILE.getFeatureName(),
                publisherUserId = publisherUserId,
                source = SourceScreen.PROFILE,
                ctaType = CtaType.FOLLOW,
            ),
        )
    }

    fun followerListViewed(
        publisherUserId: String,
        tab: FollowersListTab,
        totalCount: Long,
    ) {
        analyticsManager.trackEvent(
            FollowersListViewedEventData(
                featureName = Features.PROFILE.getFeatureName(),
                publisherUserId = publisherUserId,
                tab = tab,
                totalCount = totalCount,
            ),
        )
    }

    fun videoDownloaded(videoId: String) {
        analyticsManager.trackEvent(VideoDownloadedEventData(videoId))
    }
}
