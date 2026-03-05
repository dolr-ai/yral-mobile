package com.yral.shared.data.domain

import com.yral.shared.data.domain.models.VideoViews

interface CommonApis {
    suspend fun getVideoViewsCount(videoId: List<String>): List<VideoViews>

    suspend fun softDeleteInfluencer(
        principal: String,
        idToken: String,
        chatBaseUrl: String,
    ): Result<Unit>
}
