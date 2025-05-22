package com.yral.shared.features.feed.data

import com.yral.shared.features.feed.domain.IFeedRepository
import com.yral.shared.features.feed.domain.ReportRequest
import com.yral.shared.features.feed.domain.toDto

class FeedRepository(
    private val feedRemoteDataSource: IFeedRemoteDataSource,
) : IFeedRepository {
    override suspend fun reportVideo(request: ReportRequest): String =
        feedRemoteDataSource
            .reportVideo(request.toDto())
}
