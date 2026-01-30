package com.yral.shared.data.domain.models

data class FeedDetails(
    val postID: String,
    val videoID: String,
    val canisterID: String,
    val principalID: String,
    val url: String,
    val hashtags: List<String>,
    val thumbnail: String,
    val viewCount: ULong,
    val bulkViewCount: ULong? = null,
    val displayName: String,
    val postDescription: String,
    var profileImageURL: String?,
    var likeCount: ULong,
    var isLiked: Boolean,
    var nsfwProbability: Double?,
    val isFollowing: Boolean,
    val isFromServiceCanister: Boolean,
    val userName: String?,
    val isProUser: Boolean = false,
    val isAiInfluencer: Boolean? = null,
) {
    fun isNSFW(): Boolean = (nsfwProbability ?: 0.0) > NSFW_PROBABILITY

    companion object {
        const val NSFW_PROBABILITY = 0.4
    }
}
