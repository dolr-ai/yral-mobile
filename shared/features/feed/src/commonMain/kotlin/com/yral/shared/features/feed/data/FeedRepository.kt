package com.yral.shared.features.feed.data

import com.yral.shared.features.feed.data.models.toPostResponse
import com.yral.shared.features.feed.domain.IFeedRepository
import com.yral.shared.features.feed.domain.models.AIFeedRequest
import com.yral.shared.features.feed.domain.models.FeedRequest
import com.yral.shared.features.feed.domain.models.PostResponse
import com.yral.shared.features.feed.domain.models.toDTO
import com.yral.shared.features.feed.domain.models.toDto

class FeedRepository(
    private val feedRemoteDataSource: IFeedDataSource,
) : IFeedRepository {
    override suspend fun getInitialFeeds(feedRequest: FeedRequest): PostResponse =
        feedRemoteDataSource
            .getInitialFeeds(feedRequest.toDTO())
            .toPostResponse()

    override suspend fun fetchMoreFeeds(feedRequest: FeedRequest): PostResponse =
        feedRemoteDataSource
            .fetchMoreFeeds(feedRequest.toDTO())
            .toPostResponse()

    override suspend fun fetchAIFeeds(feedRequest: AIFeedRequest): PostResponse =
        feedRemoteDataSource
            .fetchAIFeeds(feedRequest.toDto())
            .toPostResponse()

    override suspend fun getInitialCachedFeeds(feedRequest: FeedRequest): PostResponse =
        feedRemoteDataSource
            .getInitialCachedFeeds(feedRequest.toDTO())
            .toPostResponse()

    override suspend fun getTournamentFeeds(
        tournamentId: String,
        withMetadata: Boolean,
    ): PostResponse =
        feedRemoteDataSource
            .getTournamentFeeds(
                tournamentId = tournamentId,
                withMetadata = withMetadata,
            ).toPostResponse()
}
