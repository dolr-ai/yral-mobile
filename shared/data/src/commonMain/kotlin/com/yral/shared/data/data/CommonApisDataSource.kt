package com.yral.shared.data.data

import com.yral.shared.data.data.models.VideoViewsDto

interface CommonApisDataSource {
    suspend fun getVideoViewsCount(videoId: List<String>): List<VideoViewsDto>
}
