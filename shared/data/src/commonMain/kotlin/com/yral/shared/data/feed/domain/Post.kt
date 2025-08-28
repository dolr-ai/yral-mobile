package com.yral.shared.data.feed.domain

import com.yral.shared.data.feed.data.PostDTO

data class Post(
    val canisterID: String,
    val publisherUserId: String,
    val postID: Long,
    val videoID: String,
    val nsfwProbability: Double?,
)

fun Post.toDTO(): PostDTO =
    PostDTO(
        canisterID = canisterID,
        publisherUserId = publisherUserId,
        postID = postID,
        videoID = videoID,
        nsfwProbability = nsfwProbability,
    )
