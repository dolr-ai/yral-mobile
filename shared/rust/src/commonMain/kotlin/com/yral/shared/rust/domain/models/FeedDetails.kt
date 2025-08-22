package com.yral.shared.rust.domain.models

import io.ktor.http.Url

data class FeedDetails(
    val postID: Long,
    val videoID: String,
    val canisterID: String,
    val principalID: String,
    val url: Url,
    val hashtags: List<String>,
    val thumbnail: Url,
    val viewCount: ULong,
    val displayName: String,
    val postDescription: String,
    var profileImageURL: Url?,
    var likeCount: ULong,
    var isLiked: Boolean,
    var nsfwProbability: Double?,
) {
    fun isNSFW(): Boolean = (nsfwProbability ?: 0.0) > NSFW_PROBABILITY

    companion object {
        const val NSFW_PROBABILITY = 0.4
    }
}
