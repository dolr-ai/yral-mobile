package com.yral.shared.rust.service.domain.models

import com.yral.shared.core.exceptions.YralException
import com.yral.shared.core.utils.resolveUsername
import com.yral.shared.data.domain.models.FeedDetails
import com.yral.shared.data.domain.models.Post
import com.yral.shared.rust.service.data.IndividualUserDataSourceImpl.Companion.thumbnailUrl
import com.yral.shared.rust.service.data.IndividualUserDataSourceImpl.Companion.videoUrl
import com.yral.shared.rust.service.utils.CanisterData
import com.yral.shared.rust.service.utils.getUserInfoServiceCanister
import com.yral.shared.rust.service.utils.propicFromPrincipal
import com.yral.shared.uniffi.generated.PostDetailsForFrontend
import com.yral.shared.uniffi.generated.PostDetailsWithUserInfo
import com.yral.shared.uniffi.generated.PostStatus
import com.yral.shared.uniffi.generated.UpsPostDetailsForFrontend
import com.yral.shared.uniffi.generated.UpsPostStatus

internal fun PostDetailsForFrontend.toFeedDetails(
    postId: String,
    canisterId: String,
    nsfwProbability: Double?,
): FeedDetails {
    if (status == PostStatus.BANNED_DUE_TO_USER_REPORTING) {
        throw YralException("Post is banned $postId")
    }
    val profileImageUrl = propicFromPrincipal(createdByUserPrincipalId)
    return FeedDetails(
        postID = postId,
        videoID = videoUid,
        canisterID = canisterId,
        principalID = createdByUserPrincipalId,
        url = videoUrl(videoUid),
        hashtags = hashtags,
        thumbnail = thumbnailUrl(videoUid),
        viewCount = totalViewCount,
        displayName = createdByDisplayName ?: "",
        postDescription = description,
        profileImageURL = profileImageUrl,
        likeCount = likeCount,
        isLiked = likedByMe,
        nsfwProbability = nsfwProbability,
        isFollowing = false,
        isFromServiceCanister = false,
        userName = null,
    )
}

internal fun UpsPostDetailsForFrontend.toFeedDetails(
    postId: String,
    canisterId: String,
    nsfwProbability: Double?,
): FeedDetails {
    if (status == UpsPostStatus.BANNED_DUE_TO_USER_REPORTING) {
        throw YralException("Post is banned $postId")
    }
    val profileImageUrl = propicFromPrincipal(createdByUserPrincipalId)
    return FeedDetails(
        postID = postId,
        videoID = videoUid,
        canisterID = canisterId,
        principalID = createdByUserPrincipalId,
        url = videoUrl(videoUid),
        hashtags = hashtags,
        thumbnail = thumbnailUrl(videoUid),
        viewCount = totalViewCount,
        displayName = "",
        postDescription = description,
        profileImageURL = profileImageUrl,
        likeCount = likeCount,
        isLiked = likedByMe,
        nsfwProbability = nsfwProbability,
        isFollowing = false,
        isFromServiceCanister = true,
        userName = null,
    )
}

internal fun PostDetailsWithUserInfo.toFeedDetails(): FeedDetails =
    FeedDetails(
        postID = postId,
        videoID = uid,
        canisterID = canisterId,
        principalID = posterPrincipal,
        url = videoUrl(uid),
        hashtags = hashtags,
        thumbnail = thumbnailUrl(uid),
        viewCount = views,
        displayName = displayName ?: username ?: "",
        postDescription = description,
        profileImageURL = propicUrl,
        likeCount = likes,
        isLiked = likedByUser ?: false,
        nsfwProbability = nsfwProbability.toDouble(),
        isFollowing = userFollowsCreator ?: false,
        isFromServiceCanister = canisterId == getUserInfoServiceCanister(),
        userName = username,
    )

fun FeedDetails.toCanisterData(): CanisterData =
    CanisterData(
        canisterId = canisterID,
        userPrincipalId = principalID,
        profilePic = profileImageURL ?: "",
        username = resolveUsername(userName, principalID),
        isCreatedFromServiceCanister = isFromServiceCanister,
        isFollowing = isFollowing,
    )

fun Post.toPartialFeedDetails(): FeedDetails =
    FeedDetails(
        postID = postID,
        videoID = videoID,
        canisterID = canisterID,
        principalID = publisherUserId,
        url = videoUrl(videoID),
        hashtags = emptyList(),
        thumbnail = thumbnailUrl(videoID),
        viewCount = numViewsAll ?: 0u,
        displayName = "",
        postDescription = "",
        profileImageURL = propicFromPrincipal(publisherUserId),
        likeCount = 0u,
        isLiked = false,
        nsfwProbability = nsfwProbability,
        isFollowing = false,
        isFromServiceCanister = canisterID == getUserInfoServiceCanister(),
        userName = null,
    )
