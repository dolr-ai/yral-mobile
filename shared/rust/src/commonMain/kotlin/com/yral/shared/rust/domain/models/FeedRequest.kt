package com.yral.shared.rust.domain.models

import com.yral.shared.rust.data.models.FeedRequestDTO
import com.yral.shared.rust.data.models.FilteredResultDTO

data class FeedRequest(
    val userId: String,
    val filterResults: List<FilteredResult>,
    val numResults: Long,
)

data class FilteredResult(
    val canisterID: String,
    val publisherUserId: String,
    val postID: Long,
    val videoID: String,
    val nsfwProbability: Double?,
    val isNSFW: Boolean?,
)

fun FeedRequest.toDTO(): FeedRequestDTO =
    FeedRequestDTO(
        userId = userId,
        filterResults =
            filterResults.map {
                FilteredResultDTO(
                    canisterID = it.canisterID,
                    userId = it.publisherUserId,
                    postID = it.postID,
                    videoID = it.videoID,
                    nsfwProbability = it.nsfwProbability,
                    isNSFW = it.isNSFW,
                )
            },
        numResults = numResults,
    )
