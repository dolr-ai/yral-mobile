package com.yral.shared.rust.service.domain.models

import co.touchlab.kermit.Logger
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.data.domain.models.FeedDetails
import com.yral.shared.http.spacetime.SpacetimePostDetails
import com.yral.shared.http.spacetime.SpacetimePostListOffset
import com.yral.shared.http.spacetime.SpacetimePostStatus
import com.yral.shared.rust.service.data.IndividualUserDataSourceImpl.Companion.thumbnailUrl
import com.yral.shared.rust.service.data.IndividualUserDataSourceImpl.Companion.videoUrl

private val logger = Logger.withTag("SpacetimeFeedMappings")

/**
 * Map a `SpacetimePostDetails` (from SpacetimeDB REST) to the domain
 * `FeedDetails` model.
 *
 * This replaces the old `UpsPostDetailsForFrontend.toFeedDetails()` mapping
 * that went through the Rust FFI / IC canister.
 */
internal fun SpacetimePostDetails.toFeedDetails(
    postId: String,
    canisterId: String,
    nsfwProbability: Double?,
): FeedDetails {
    if (status == SpacetimePostStatus.BannedDueToUserReporting ||
        status == SpacetimePostStatus.BannedForExplicitness
    ) {
        throw YralException("Post is banned $postId")
    }
    return FeedDetails(
        postID = postId,
        videoID = videoUid,
        canisterID = canisterId,
        principalID = creatorPrincipalText,
        url = videoUrl(videoUid, creatorPrincipalText),
        hashtags = hashtags,
        thumbnail = thumbnailUrl(videoUid, creatorPrincipalText),
        viewCount = totalViewCount,
        bulkViewCount = totalViewCount,
        displayName = "",
        postDescription = description,
        profileImageURL = null, // Profile pic is fetched separately via getUserProfileDetailsV7
        likeCount = likeCount,
        isLiked = likedByMe,
        nsfwProbability = nsfwProbability,
        isFollowing = false,
        isFromServiceCanister = true,
        userName = null,
        isDraft = status == SpacetimePostStatus.Draft,
    )
}

/**
 * Map a `SpacetimePostListOffset` (from SpacetimeDB REST) to the domain
 * `Posts` model. Banned posts are filtered out (matching the old IC behavior).
 */
internal fun SpacetimePostListOffset.toPosts(canisterId: String): Posts {
    val feedDetails =
        posts.mapNotNull { post ->
            try {
                post.toFeedDetails(post.id, canisterId, 0.0)
            } catch (exception: YralException) {
                // Skip banned posts — log the post id and exception message for debugging
                logger.d { "toPosts: skipping banned post ${post.id}: ${exception.message}" }
                null
            }
        }
    return Posts.Ok(feedDetails)
}
