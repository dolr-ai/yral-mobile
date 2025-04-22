package com.yral.shared.rust.domain.models

data class PostResponse(
    val posts: List<CachedPost>,
)

data class CachedPost(
    val canisterID: String,
    val postID: Long,
    val videoID: String,
    val nsfwProbability: Double,
)

fun CachedPost.toFilteredResult(): FilteredResult =
    FilteredResult(
        canisterID = canisterID,
        postID = postID,
        videoID = videoID,
        nsfwProbability = nsfwProbability,
    )
