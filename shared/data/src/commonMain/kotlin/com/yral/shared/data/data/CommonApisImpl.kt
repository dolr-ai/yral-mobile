package com.yral.shared.data.data

import com.yral.shared.data.data.models.toDomain
import com.yral.shared.data.domain.CommonApis
import com.yral.shared.data.domain.models.VideoViews

class CommonApisImpl(
    val remoteDataSource: CommonApisDataSource,
) : CommonApis {
    override suspend fun getVideoViewsCount(videoId: List<String>): List<VideoViews> =
        remoteDataSource
            .getVideoViewsCount(videoId)
            .map { it.toDomain() }

    override suspend fun softDeleteInfluencer(
        principal: String,
        idToken: String,
        environmentPrefix: String,
    ): Result<Unit> = remoteDataSource.softDeleteInfluencer(principal, idToken, environmentPrefix)
}
