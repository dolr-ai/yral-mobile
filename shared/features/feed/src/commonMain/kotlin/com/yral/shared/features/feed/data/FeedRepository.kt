package com.yral.shared.features.feed.data

import com.yral.shared.features.feed.data.models.toPostResponse
import com.yral.shared.features.feed.domain.IFeedRepository
import com.yral.shared.features.feed.domain.ReportRequest
import com.yral.shared.features.feed.domain.models.FeedRequest
import com.yral.shared.features.feed.domain.models.PostResponse
import com.yral.shared.features.feed.domain.models.toDTO
import com.yral.shared.features.feed.domain.toDto

class FeedRepository(
    private val feedRemoteDataSource: IFeedRemoteDataSource,
) : IFeedRepository {
    override suspend fun getInitialFeeds(feedRequest: FeedRequest): PostResponse =
        feedRemoteDataSource
            .getInitialFeeds(feedRequest.toDTO())
            .toPostResponse()

    override suspend fun fetchMoreFeeds(feedRequest: FeedRequest): PostResponse =
        feedRemoteDataSource
            .fetchMoreFeeds(feedRequest.toDTO())
            .toPostResponse()

    override suspend fun reportVideo(request: ReportRequest): String =
        feedRemoteDataSource
            .reportVideo(request.toDto())
}
