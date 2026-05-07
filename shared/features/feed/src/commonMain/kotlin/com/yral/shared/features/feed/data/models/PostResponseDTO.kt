package com.yral.shared.features.feed.data.models

import com.yral.shared.data.data.models.PostDTO
import com.yral.shared.data.domain.models.Post
import com.yral.shared.features.feed.domain.models.PostResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PostResponseDTO(
    val posts: List<PostDTO>,
    @SerialName("processing_time_ms")
    val processingTimeMs: Double? = null,
//    not adding error for now as type is unknown
//    val error: String? = null,
)

fun PostResponseDTO.toPostResponse(): PostResponse =
    PostResponse(
        posts =
            posts.map {
                it.toPost()
            },
    )

fun PostDTO.toPost(): Post =
    Post(
        canisterID = canisterID,
        publisherUserId = publisherUserId,
        postID = postID,
        videoID = videoID,
        nsfwProbability = nsfwProbability,
        numViewsAll = numViewsAll,
        numViewsLoggedIn = numViewsLoggedIn,
        fromAiInfluencer = fromAiInfluencer,
        isFollowing = isFollowing,
        username = username,
        isProUser = isProUser,
        profileImageUrl = profileImageUrl,
    )
