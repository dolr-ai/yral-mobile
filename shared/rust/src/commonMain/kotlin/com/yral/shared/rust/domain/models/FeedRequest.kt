package com.yral.shared.rust.domain.models

import com.yral.shared.rust.data.models.FeedRequestDTO
import com.yral.shared.rust.data.models.FilteredResultDTO

data class FeedRequest(
    val canisterID: String,
    val filterResults: List<FilteredResult>,
    val numResults: Long,
)

data class FilteredResult(
    val canisterID: String,
    val postID: Long,
    val videoID: String,
    val nsfwProbability: Double,
)

fun FeedRequest.toDTO(): FeedRequestDTO =
    FeedRequestDTO(
        canisterID = canisterID,
        filterResults =
            filterResults.map {
                FilteredResultDTO(
                    canisterID = it.canisterID,
                    postID = it.postID,
                    videoID = it.videoID,
                    nsfwProbability = it.nsfwProbability,
                )
            },
        numResults = numResults,
    )
