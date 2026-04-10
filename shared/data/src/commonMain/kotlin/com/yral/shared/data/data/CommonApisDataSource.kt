package com.yral.shared.data.data

import com.yral.shared.data.data.models.DailyStreakDto
import com.yral.shared.data.data.models.VideoViewsDto

interface CommonApisDataSource {
    suspend fun getVideoViewsCount(videoId: List<String>): List<VideoViewsDto>

    suspend fun softDeleteInfluencer(
        principal: String,
        idToken: String,
        chatBaseUrl: String,
    ): Result<Unit>

    suspend fun fetchDailyStreak(
        userPrincipal: String,
        idToken: String,
    ): DailyStreakDto
}
