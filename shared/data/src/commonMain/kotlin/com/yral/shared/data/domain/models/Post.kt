package com.yral.shared.data.domain.models

import com.yral.shared.data.data.models.PostDTO

data class Post(
    val canisterID: String,
    val publisherUserId: String,
    val postID: String,
    val videoID: String,
    val nsfwProbability: Double?,
    val numViewsLoggedIn: ULong?,
    val numViewsAll: ULong?,
)

fun Post.toDTO(): PostDTO =
    PostDTO(
        canisterID = canisterID,
        publisherUserId = publisherUserId,
        postID = postID,
        videoID = videoID,
        nsfwProbability = nsfwProbability,
    )
