package com.yral.shared.data.domain

import com.yral.shared.data.domain.models.VideoViews

interface CommonApis {
    suspend fun getVideoViewsCount(videoId: List<String>): List<VideoViews>
}
