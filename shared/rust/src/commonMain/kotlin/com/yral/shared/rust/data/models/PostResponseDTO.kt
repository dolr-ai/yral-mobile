package com.yral.shared.rust.data.models

import com.yral.shared.rust.domain.models.CachedPost
import com.yral.shared.rust.domain.models.PostResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PostResponseDTO(
    val posts: List<CacheDTO>,
)

@Serializable
data class CacheDTO(
    @SerialName("canister_id")
    val canisterID: String,
    @SerialName("post_id")
    val postID: Long,
    @SerialName("video_id")
    val videoID: String,
    @SerialName("nsfw_probability")
    val nsfwProbability: Double,
)

fun PostResponseDTO.toPostResponse(): PostResponse =
    PostResponse(
        posts =
            posts.map {
                CachedPost(
                    canisterID = it.canisterID,
                    postID = it.postID,
                    videoID = it.videoID,
                    nsfwProbability = it.nsfwProbability,
                )
            },
    )
