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
    val fromAiInfluencer: Boolean? = null,
    val isFollowing: Boolean? = null,
    val username: String? = null,
    val isProUser: Boolean? = null,
    val profileImageUrl: String? = null,
)

fun Post.toDTO(): PostDTO =
    PostDTO(
        canisterID = canisterID,
        publisherUserId = publisherUserId,
        postID = postID,
        videoID = videoID,
        nsfwProbability = nsfwProbability,
        numViewsLoggedIn = numViewsLoggedIn,
        numViewsAll = numViewsAll,
        fromAiInfluencer = fromAiInfluencer,
        isFollowing = isFollowing,
        username = username,
        isProUser = isProUser,
        profileImageUrl = profileImageUrl,
    )
