package com.yral.shared.features.feed.domain.models

import com.yral.shared.features.feed.data.models.FeedRequestDTO

data class FeedRequest(
    val userId: String,
    val numResults: Long,
    val isNSFW: Boolean = false,
    val excludeItems: List<String> = emptyList(),
)

data class FilteredResult(
    val canisterID: String,
    val publisherUserId: String,
    val postID: Long,
    val videoID: String,
    val nsfwProbability: Double?,
)

fun FeedRequest.toDTO(): FeedRequestDTO =
    FeedRequestDTO(
        userId = userId,
        isNSFW = isNSFW,
        numResults = numResults,
        excludeItems = excludeItems,
    )
