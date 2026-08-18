package com.yral.shared.rust.service.domain.models

import com.yral.shared.core.session.CanisterData
import com.yral.shared.core.utils.propicFromPrincipal
import com.yral.shared.core.utils.resolveUsername
import com.yral.shared.data.domain.models.FeedDetails
import com.yral.shared.data.domain.models.Post
import com.yral.shared.rust.service.data.IndividualUserDataSourceImpl.Companion.thumbnailUrl
import com.yral.shared.rust.service.data.IndividualUserDataSourceImpl.Companion.videoUrl

fun FeedDetails.toCanisterData(): CanisterData =
    CanisterData(
        canisterId = canisterID,
        userPrincipalId = principalID,
        profilePic = profileImageURL ?: "",
        username = resolveUsername(userName, principalID),
        isCreatedFromServiceCanister = isFromServiceCanister,
        isFollowing = isFollowing,
    )

fun Post.toPartialFeedDetails(
    isFromServiceCanister: Boolean = true,
    profileImageUrlFallback: String = propicFromPrincipal(publisherUserId),
): FeedDetails =
    FeedDetails(
        postID = postID,
        videoID = videoID,
        canisterID = canisterID,
        principalID = publisherUserId,
        url =
            videoUrl(
                videoID,
                publisherUserId = publisherUserId,
            ),
        hashtags = emptyList(),
        thumbnail = thumbnailUrl(videoID, publisherUserId),
        viewCount = numViewsAll ?: 0u,
        bulkViewCount = numViewsAll,
        displayName = username ?: "",
        postDescription = "",
        profileImageURL =
            profileImageUrl
                ?.takeIf { it.isNotBlank() }
                ?: profileImageUrlFallback,
        likeCount = 0u,
        isLiked = false,
        nsfwProbability = nsfwProbability,
        isFollowing = isFollowing ?: false,
        isFromServiceCanister = isFromServiceCanister,
        userName = username,
        isProUser = isProUser ?: false,
        isAiInfluencer = fromAiInfluencer,
    )
