package com.yral.shared.features.feed.data.models

import com.yral.shared.data.feed.data.PostDTO
import com.yral.shared.data.feed.domain.Post
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
                Post(
                    canisterID = it.canisterID,
                    publisherUserId = it.publisherUserId,
                    postID = it.postID,
                    videoID = it.videoID,
                    nsfwProbability = it.nsfwProbability,
                    numViewsAll = it.numViewsAll,
                    numViewsLoggedIn = it.numViewsLoggedIn,
                )
            },
    )
