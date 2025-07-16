package com.yral.shared.features.feed.analytics

import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.analytics.events.CategoryName
import com.yral.shared.analytics.events.CtaType
import com.yral.shared.analytics.events.GameType
import com.yral.shared.analytics.events.HomePageViewedEventData
import com.yral.shared.analytics.events.VideoClickedEventData
import com.yral.shared.analytics.events.VideoDurationWatchedEventData
import com.yral.shared.analytics.events.VideoImpressionEventData
import com.yral.shared.analytics.events.VideoReportedEventData
import com.yral.shared.analytics.events.VideoStartedEventData
import com.yral.shared.analytics.events.VideoViewedEventData
import com.yral.shared.core.AppConfigurations.NSFW_PROBABILITY
import com.yral.shared.core.session.SessionManager
import com.yral.shared.features.feed.viewmodel.VideoReportReason
import com.yral.shared.features.feed.viewmodel.percentageOf
import com.yral.shared.rust.domain.models.FeedDetails

class FeedTelemetry(
    private val analyticsManager: AnalyticsManager,
    private val sessionManager: SessionManager,
) {
    private val trackedImpressions = mutableSetOf<String>()
    private val trackedStated = mutableSetOf<String>()
    fun onFeedPageViewed() {
        analyticsManager.trackEvent(HomePageViewedEventData())
    }

    suspend fun onVideoDurationWatched(
        feedDetails: FeedDetails,
        isLoggedIn: Boolean,
        currentTime: Int,
        totalTime: Int,
    ) {
        val eventData =
            feedDetails
                .toVideoEventData()
                .copy(
                    canisterId = sessionManager.canisterID ?: "",
                    userID = sessionManager.userPrincipal ?: "",
                    isLoggedIn = isLoggedIn,
                    absoluteWatched = currentTime.toDouble(),
                    percentageWatched = currentTime.percentageOf(totalTime),
                    videoDuration = totalTime.toDouble(),
                )
        analyticsManager.trackEvent(eventData)
    }

    private fun FeedDetails.toVideoEventData(): VideoDurationWatchedEventData =
        VideoDurationWatchedEventData(
            displayName = displayName,
            hashtagCount = hashtags.size,
            isHotOrNot = nsfwProbability > NSFW_PROBABILITY,
            isNsfw = false,
            likeCount = likeCount.toLong(),
            postID = postID,
            publisherCanisterId = canisterID,
            publisherUserId = principalID,
            videoID = videoID,
            viewCount = viewCount.toLong(),
            nsfwProbability = nsfwProbability,
            isLoggedIn = false,
            canisterId = "",
            userID = "",
        )

    fun trackVideoImpression(feedDetails: FeedDetails) {
        if (trackedImpressions.contains(feedDetails.videoID)) return
        trackedImpressions += feedDetails.videoID
        analyticsManager.trackEvent(
            event =
                VideoImpressionEventData(
                    categoryName = CategoryName.HOME,
                    videoId = feedDetails.videoID,
                    publisherUserId = feedDetails.principalID,
                    likeCount = feedDetails.likeCount.toLong(),
                    viewCount = feedDetails.viewCount.toLong(),
                    isNsfw = feedDetails.nsfwProbability > NSFW_PROBABILITY,
                    shareCount = 0,
                    isGameEnabled = true,
                    gameType = GameType.SMILEY,
                ),
        )
    }

    fun trackVideoStarted(feedDetails: FeedDetails) {
        if (trackedStated.contains(feedDetails.videoID)) return
        // trackedStated += feedDetails.videoID
        analyticsManager.trackEvent(
            event =
                VideoStartedEventData(
                    videoId = feedDetails.videoID,
                    publisherUserId = feedDetails.principalID,
                    likeCount = feedDetails.likeCount.toLong(),
                    viewCount = feedDetails.viewCount.toLong(),
                    isNsfw = feedDetails.nsfwProbability > NSFW_PROBABILITY,
                    shareCount = 0,
                    isGameEnabled = true,
                    gameType = GameType.SMILEY,
                ),
        )
    }

    fun trackVideoViewed(feedDetails: FeedDetails) {
        analyticsManager.trackEvent(
            event =
                VideoViewedEventData(
                    videoId = feedDetails.videoID,
                    publisherUserId = feedDetails.principalID,
                    likeCount = feedDetails.likeCount.toLong(),
                    viewCount = feedDetails.viewCount.toLong(),
                    isNsfw = feedDetails.nsfwProbability > NSFW_PROBABILITY,
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
                    videoId = feedDetails.videoID,
                    publisherUserId = feedDetails.principalID,
                    likeCount = feedDetails.likeCount.toLong(),
                    viewCount = feedDetails.viewCount.toLong(),
                    isNsfw = feedDetails.nsfwProbability > NSFW_PROBABILITY,
                    ctaType = ctaType,
                    shareCount = 0,
                    isGameEnabled = true,
                    gameType = GameType.SMILEY,
                    pageName = CategoryName.HOME,
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
                    isNsfw = feedDetails.nsfwProbability > NSFW_PROBABILITY,
                    isGameEnabled = true,
                    gameType = GameType.SMILEY,
                    reason = reason.reason,
                ),
        )
    }
}
