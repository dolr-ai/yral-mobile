package com.yral.shared.data.feed.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PostDTO(
    @SerialName("canister_id")
    val canisterID: String,
    @SerialName("publisher_user_id")
    val publisherUserId: String,
    @SerialName("post_id")
    val postID: String,
    @SerialName("video_id")
    val videoID: String,
    @SerialName("nsfw_probability")
    val nsfwProbability: Double? = null,
)
