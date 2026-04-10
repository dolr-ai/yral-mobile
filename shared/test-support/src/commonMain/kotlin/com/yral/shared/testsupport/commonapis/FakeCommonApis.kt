package com.yral.shared.testsupport.commonapis

import com.yral.shared.data.domain.CommonApis
import com.yral.shared.data.domain.models.DailyStreak
import com.yral.shared.data.domain.models.VideoViews

class FakeCommonApis(
    private val dailyStreak: DailyStreak? = null,
) : CommonApis {
    var lastRequestedPrincipal: String? = null

    override suspend fun getVideoViewsCount(videoId: List<String>): List<VideoViews> = emptyList()

    override suspend fun softDeleteInfluencer(
        principal: String,
        idToken: String,
        chatBaseUrl: String,
    ): Result<Unit> = Result.success(Unit)

    override suspend fun fetchDailyStreak(
        userPrincipal: String,
        idToken: String,
    ): DailyStreak {
        lastRequestedPrincipal = userPrincipal
        return dailyStreak ?: throw NotImplementedError()
    }
}
