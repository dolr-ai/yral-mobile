package com.yral.shared.rust.data.models

import com.yral.shared.rust.domain.models.Post
import com.yral.shared.rust.domain.models.PostResponse
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

@Serializable
data class PostDTO(
    @SerialName("canister_id")
    val canisterID: String,
    @SerialName("publisher_user_id")
    val publisherUserId: String,
    @SerialName("post_id")
    val postID: Long,
    @SerialName("video_id")
    val videoID: String,
    @SerialName("nsfw_probability")
    val nsfwProbability: Double? = null,
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
                )
            },
    )
