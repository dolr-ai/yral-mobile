package com.yral.shared.data.feed.data

import com.yral.shared.data.feed.domain.CommonApis
import com.yral.shared.data.feed.domain.VideoViews

class CommonApisImpl(
    val remoteDataSource: CommonApisDataSource,
) : CommonApis {
    override suspend fun getVideoViewsCount(videoId: List<String>): List<VideoViews> =
        remoteDataSource
            .getVideoViewsCount(videoId)
            .map { it.toDomain() }
}
