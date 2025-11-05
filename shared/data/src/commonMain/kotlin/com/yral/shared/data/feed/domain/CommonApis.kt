package com.yral.shared.data.feed.domain

interface CommonApis {
    suspend fun getVideoViewsCount(videoId: List<String>): List<VideoViews>
}
