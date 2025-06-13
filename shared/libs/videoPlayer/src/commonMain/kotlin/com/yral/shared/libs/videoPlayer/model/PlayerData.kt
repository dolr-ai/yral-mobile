package com.yral.shared.libs.videoPlayer.model

data class PlayerData(
    val videoId: String,
    val url: String,
    val thumbnailUrl: String,
    val prefetchThumbnails: List<String>,
)
