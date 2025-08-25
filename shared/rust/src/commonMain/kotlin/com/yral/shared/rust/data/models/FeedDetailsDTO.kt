package com.yral.shared.rust.data.models

import com.yral.shared.core.exceptions.YralException
import com.yral.shared.rust.data.IndividualUserDataSourceImpl.Companion.CLOUD_FLARE_PREFIX
import com.yral.shared.rust.data.IndividualUserDataSourceImpl.Companion.CLOUD_FLARE_SUFFIX_MP4
import com.yral.shared.rust.data.IndividualUserDataSourceImpl.Companion.THUMBNAIL_SUFFIX
import com.yral.shared.rust.domain.models.FeedDetails
import com.yral.shared.uniffi.generated.PostDetailsForFrontend
import com.yral.shared.uniffi.generated.PostStatus
import com.yral.shared.uniffi.generated.propicFromPrincipal
import io.ktor.http.Url

fun PostDetailsForFrontend.toFeedDetails(
    postId: Long,
    canisterId: String,
    nsfwProbability: Double?,
): FeedDetails {
    if (status == PostStatus.BANNED_DUE_TO_USER_REPORTING) {
        throw YralException("Post is banned $postId")
    }
    val videoUrl = Url("$CLOUD_FLARE_PREFIX$videoUid$CLOUD_FLARE_SUFFIX_MP4")
    val thumbnailUrl = Url("$CLOUD_FLARE_PREFIX$videoUid$THUMBNAIL_SUFFIX")
    val profileImageUrl = Url(propicFromPrincipal(createdByUserPrincipalId))
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
