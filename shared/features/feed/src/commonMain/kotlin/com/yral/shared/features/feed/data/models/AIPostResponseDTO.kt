package com.yral.shared.features.feed.data.models

import com.yral.shared.data.feed.data.PostDTO
import com.yral.shared.data.feed.domain.Post
import com.yral.shared.features.feed.domain.models.PostResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AIPostResponseDTO(
    @SerialName("user_id")
    val userId: String,
    @SerialName("videos")
    val videos: List<PostDTO>,
    val count: Int,
)

fun AIPostResponseDTO.toPostResponse(): PostResponse =
    PostResponse(
        posts =
            videos.map {
                Post(
                    canisterID = it.canisterID,
                    publisherUserId = it.publisherUserId,
                    postID = it.postID,
                    videoID = it.videoID,
                    nsfwProbability = it.nsfwProbability,
                )
            },
    )
