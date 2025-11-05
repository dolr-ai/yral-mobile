package com.yral.shared.data.feed.data

interface CommonApisDataSource {
    suspend fun getVideoViewsCount(videoId: List<String>): List<VideoViewsDto>
}
