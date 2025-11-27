package com.yral.shared.features.feed.data.models

import com.yral.shared.data.domain.models.FeedDetails
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
@Serializable
internal data class FeedDetailsCache(
    @SerialName("timestamp") val timestamp: Instant,
    @SerialName("feedDetails") val feedDetails: List<FeedDetailsForCache>,
)

@Serializable
internal data class FeedDetailsForCache(
    @SerialName("postID") val postID: String,
    @SerialName("videoID") val videoID: String,
    @SerialName("canisterID") val canisterID: String,
    @SerialName("principalID") val principalID: String,
    @SerialName("url") val url: String,
    @SerialName("hashtags") val hashtags: List<String>,
    @SerialName("thumbnail") val thumbnail: String,
    @SerialName("viewCount") val viewCount: ULong,
    @SerialName("displayName") val displayName: String,
    @SerialName("postDescription") val postDescription: String,
    @SerialName("profileImageURL") var profileImageURL: String?,
    @SerialName("likeCount") var likeCount: ULong,
    @SerialName("isLiked") var isLiked: Boolean,
    @SerialName("nsfwProbability") var nsfwProbability: Double?,
    @SerialName("isFollowing") val isFollowing: Boolean,
    @SerialName("isFromServiceCanister") val isFromServiceCanister: Boolean,
    @SerialName("userName") val userName: String?,
)

internal fun FeedDetailsForCache.toFeedDetails(): FeedDetails =
    FeedDetails(
        postID = postID,
        videoID = videoID,
        canisterID = canisterID,
        principalID = principalID,
        url = url,
        hashtags = hashtags,
        thumbnail = thumbnail,
        viewCount = viewCount,
        displayName = displayName,
        postDescription = postDescription,
        profileImageURL = profileImageURL,
        likeCount = likeCount,
        isLiked = isLiked,
        nsfwProbability = nsfwProbability,
        isFollowing = isFollowing,
        isFromServiceCanister = isFromServiceCanister,
        userName = userName,
    )

internal fun FeedDetails.toFeedDetailsForCache(): FeedDetailsForCache =
    FeedDetailsForCache(
        postID = postID,
        videoID = videoID,
        canisterID = canisterID,
        principalID = principalID,
        url = url,
        hashtags = hashtags,
        thumbnail = thumbnail,
        viewCount = viewCount,
        displayName = displayName,
        postDescription = postDescription,
        profileImageURL = profileImageURL,
        likeCount = likeCount,
        isLiked = isLiked,
        nsfwProbability = nsfwProbability,
        isFollowing = isFollowing,
        isFromServiceCanister = isFromServiceCanister,
        userName = userName,
    )
