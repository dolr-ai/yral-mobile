package com.yral.shared.features.feed.analytics

import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.analytics.events.HomePageViewedEventData
import com.yral.shared.analytics.events.VideoDurationWatchedEventData
import com.yral.shared.core.session.SessionManager
import com.yral.shared.features.feed.viewmodel.FeedViewModel.Companion.NSFW_PROBABILITY
import com.yral.shared.features.feed.viewmodel.percentageOf
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
import com.yral.shared.rust.domain.models.FeedDetails

class FeedTelemetry(
    private val analyticsManager: AnalyticsManager,
    private val sessionManager: SessionManager,
    private val preferences: Preferences,
) {
    fun onFeedPageViewed() {
        analyticsManager.trackEvent(HomePageViewedEventData())
    }

    suspend fun onVideoDurationWatched(
        feedDetails: FeedDetails,
        currentTime: Int,
        totalTime: Int,
    ) {
        val eventData =
            feedDetails
                .toVideoEventData()
                .copy(
                    canisterId = sessionManager.getCanisterPrincipal() ?: "",
                    userID = sessionManager.getUserPrincipal() ?: "",
                    isLoggedIn =
                        preferences.getBoolean(PrefKeys.SOCIAL_SIGN_IN_SUCCESSFUL.name)
                            ?: false,
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
}
