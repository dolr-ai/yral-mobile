package com.yral.shared.rust.domain.models

import com.yral.shared.rust.data.models.PostDTO

data class PostResponse(
    val posts: List<Post>,
)

data class Post(
    val canisterID: String,
    val postID: Long,
    val videoID: String,
    val nsfwProbability: Double,
)

fun Post.toFilteredResult(): FilteredResult =
    FilteredResult(
        canisterID = canisterID,
        postID = postID,
        videoID = videoID,
        nsfwProbability = nsfwProbability,
    )

fun Post.toDTO(): PostDTO =
    PostDTO(
        canisterID = canisterID,
        postID = postID,
        videoID = videoID,
        nsfwProbability = nsfwProbability,
    )
