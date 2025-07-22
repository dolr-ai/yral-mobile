package com.yral.shared.rust.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FeedRequestDTO(
    @SerialName("user_id")
    val userId: String,
    @SerialName("filter_results")
    val filterResults: List<FilteredResultDTO>,
    @SerialName("num_results")
    val numResults: Long,
)

@Serializable
data class FilteredResultDTO(
    @SerialName("canister_id")
    val canisterID: String,
    @SerialName("publisher_user_id")
    val userId: String,
    @SerialName("post_id")
    val postID: Long,
    @SerialName("video_id")
    val videoID: String,
    @SerialName("nsfw_probability")
    val nsfwProbability: Double?,
    @SerialName("is_nsfw")
    val isNSFW: Boolean?,
)
