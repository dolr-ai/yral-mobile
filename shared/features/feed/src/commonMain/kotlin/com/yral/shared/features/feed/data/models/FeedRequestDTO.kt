package com.yral.shared.features.feed.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FeedRequestDTO(
    @SerialName("user_id")
    val userId: String,
    @SerialName("nsfw_label")
    val isNSFW: Boolean = false,
    @SerialName("num_results")
    val numResults: Long,
    // excludeWatchedItems and excludeReportedItems are to be kept empty as it filtering of
    // watched items and reported items are now being handled internally:
    @SerialName("exclude_watched_items")
    val excludeWatchedItems: List<String> = emptyList(),
    @SerialName("exclude_reported_items")
    val excludeReportedItems: List<String> = emptyList(),
    // This parameter is used to filter out some videos on the go, and can be used if needed.
    @SerialName("exclude_items")
    val excludeItems: List<String> = emptyList(),
)
