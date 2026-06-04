package com.yral.shared.features.profile.videoideas.domain.models

data class VideoIdea(
    val id: String,
    val influencerId: String,
    val batchDate: String,
    val rank: Int,
    val hook: String,
    val ideaText: String,
    val status: VideoIdeaStatus,
    val usedAt: String? = null,
) {
    val isFresh: Boolean get() = status == VideoIdeaStatus.FRESH
}

enum class VideoIdeaStatus {
    FRESH,
    USED,
    UNKNOWN,
    ;

    companion object {
        fun fromApi(raw: String): VideoIdeaStatus =
            when (raw.lowercase()) {
                "fresh" -> FRESH
                "used" -> USED
                else -> UNKNOWN
            }
    }
}
