package com.yral.shared.rust.service.domain.models

import com.yral.shared.core.exceptions.YralException
import com.yral.shared.data.feed.domain.FeedDetails
import com.yral.shared.rust.service.data.IndividualUserDataSourceImpl.Companion.CLOUD_FLARE_PREFIX
import com.yral.shared.rust.service.data.IndividualUserDataSourceImpl.Companion.CLOUD_FLARE_SUFFIX_MP4
import com.yral.shared.rust.service.data.IndividualUserDataSourceImpl.Companion.THUMBNAIL_SUFFIX
import com.yral.shared.rust.service.utils.propicFromPrincipal
import com.yral.shared.uniffi.generated.PostDetailsForFrontend
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
    val videoUrl = "$CLOUD_FLARE_PREFIX$videoUid$CLOUD_FLARE_SUFFIX_MP4"
    val thumbnailUrl = "$CLOUD_FLARE_PREFIX$videoUid$THUMBNAIL_SUFFIX"
    val profileImageUrl = propicFromPrincipal(createdByUserPrincipalId)
    return FeedDetails(
        postID = postId,
        videoID = videoUid,
        canisterID = canisterId,
        principalID = createdByUserPrincipalId,
        url = videoUrl,
        hashtags = hashtags,
        thumbnail = thumbnailUrl,
        viewCount = totalViewCount,
        displayName = createdByDisplayName ?: "",
        postDescription = description,
        profileImageURL = profileImageUrl,
        likeCount = likeCount,
        isLiked = likedByMe,
        nsfwProbability = nsfwProbability,
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
    val videoUrl = "$CLOUD_FLARE_PREFIX$videoUid$CLOUD_FLARE_SUFFIX_MP4"
    val thumbnailUrl = "$CLOUD_FLARE_PREFIX$videoUid$THUMBNAIL_SUFFIX"
    val profileImageUrl = propicFromPrincipal(createdByUserPrincipalId)
    return FeedDetails(
        postID = postId,
        videoID = videoUid,
        canisterID = canisterId,
        principalID = createdByUserPrincipalId,
        url = videoUrl,
        hashtags = hashtags,
        thumbnail = thumbnailUrl,
        viewCount = totalViewCount,
        displayName = "",
        postDescription = description,
        profileImageURL = profileImageUrl,
        likeCount = likeCount,
        isLiked = likedByMe,
        nsfwProbability = nsfwProbability,
    )
}
