package com.yral.shared.analytics.events

import com.yral.shared.analytics.constants.FeatureEvents
import com.yral.shared.analytics.constants.Features
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VideoDurationWatchedEventData(
    @SerialName("event") override val event: String = FeatureEvents.VIDEO_DURATION_WATCHED.getEventName(),
    @SerialName("feature_name") override val featureName: String = Features.FEED.getFeatureName(),
    @SerialName("canister_id") val canisterId: String,
    @SerialName("creator_category") val creatorCategory: String = "",
    @SerialName("display_name") val displayName: String,
    @SerialName("feed_type") val feedType: String = "Clean",
    @SerialName("hashtag_count") val hashtagCount: Int,
    @SerialName("is_hot_or_not") val isHotOrNot: Boolean,
    @SerialName("is_logged_in") val isLoggedIn: Boolean,
    @SerialName("is_nsfw") val isNsfw: Boolean,
    @SerialName("like_count") val likeCount: Long,
    @SerialName("post_id") val postID: Long,
    @SerialName("publisher_canister_id") val publisherCanisterId: String,
    @SerialName("publisher_user_id") val publisherUserId: String,
    @SerialName("share_count") val shareCount: Long = 0,
    @SerialName("user_id") val userID: String,
    @SerialName("video_category") val videoCategory: String = "",
    @SerialName("video_id") val videoID: String,
    @SerialName("view_count") val viewCount: Long,
    @SerialName("nsfw_probability") val nsfwProbability: Double? = null,
    @SerialName("absolute_watched") val absoluteWatched: Double = 0.0,
    @SerialName("percentage_watched") val percentageWatched: Double = 0.0,
    @SerialName("video_duration") val videoDuration: Double = 0.0,
) : EventData
