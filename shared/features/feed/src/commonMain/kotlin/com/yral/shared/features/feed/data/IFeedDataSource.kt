package com.yral.shared.features.feed.data

import com.yral.shared.features.feed.data.models.FeedRequestDTO
import com.yral.shared.features.feed.data.models.PostResponseDTO

interface IFeedDataSource {
    suspend fun getInitialFeeds(feedRequestDTO: FeedRequestDTO): PostResponseDTO
    suspend fun fetchMoreFeeds(feedRequestDTO: FeedRequestDTO): PostResponseDTO
}
