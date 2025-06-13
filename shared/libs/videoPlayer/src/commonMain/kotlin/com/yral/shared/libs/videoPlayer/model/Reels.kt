package com.yral.shared.libs.videoPlayer.model

import com.yral.shared.libs.videoPlayer.util.nextN

data class Reels(
    val videoUrl: String,
    val thumbnailUrl: String,
    val videoId: String,
)

fun Reels.toPlayerData(
    urls: List<Reels>,
    page: Int,
): PlayerData =
    PlayerData(
        videoId = videoId,
        url = videoUrl,
        thumbnailUrl = thumbnailUrl,
        prefetchThumbnails =
            urls
                .nextN(page, PREFETCH_NEXT_N_THUMBNAILS)
                .map { it.thumbnailUrl },
    )

fun Reels.toPlayerData(): PlayerData =
    PlayerData(
        videoId = videoId,
        url = videoUrl,
        thumbnailUrl = thumbnailUrl,
        prefetchThumbnails = emptyList(),
    )

const val PREFETCH_NEXT_N_THUMBNAILS = 4
const val PREFETCH_NEXT_N_VIDEOS = 4
