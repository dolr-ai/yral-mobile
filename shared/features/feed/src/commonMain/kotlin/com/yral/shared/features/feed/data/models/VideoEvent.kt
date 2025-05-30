package com.yral.shared.features.feed.data.models

import com.yral.shared.analytics.events.VideoDurationWatchedEventData
import com.yral.shared.features.feed.viewmodel.FeedViewModel.Companion.NSFW_PROBABILITY
import com.yral.shared.rust.domain.models.FeedDetails

internal fun FeedDetails.toVideoEventData(): VideoDurationWatchedEventData =
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
