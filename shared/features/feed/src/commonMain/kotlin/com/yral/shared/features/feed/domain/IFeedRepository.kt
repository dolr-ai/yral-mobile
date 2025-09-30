package com.yral.shared.features.feed.domain

import com.yral.shared.features.feed.domain.models.FeedRequest
import com.yral.shared.features.feed.domain.models.PostResponse

interface IFeedRepository {
    suspend fun getInitialFeeds(feedRequest: FeedRequest): PostResponse
    suspend fun fetchMoreFeeds(feedRequest: FeedRequest): PostResponse
}
