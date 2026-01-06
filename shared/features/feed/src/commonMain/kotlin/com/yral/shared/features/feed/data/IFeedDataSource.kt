package com.yral.shared.features.feed.data

import com.yral.shared.features.feed.data.models.AIFeedRequestDto
import com.yral.shared.features.feed.data.models.AIPostResponseDTO
import com.yral.shared.features.feed.data.models.FeedRequestDTO
import com.yral.shared.features.feed.data.models.GlobalCachePostResponseDTO
import com.yral.shared.features.feed.data.models.PostResponseDTO
import com.yral.shared.features.feed.data.models.TournamentPostResponseDTO

interface IFeedDataSource {
    suspend fun getInitialFeeds(feedRequestDTO: FeedRequestDTO): PostResponseDTO
    suspend fun fetchMoreFeeds(feedRequestDTO: FeedRequestDTO): PostResponseDTO
    suspend fun fetchAIFeeds(feedRequest: AIFeedRequestDto): AIPostResponseDTO
    suspend fun getInitialCachedFeeds(feedRequestDTO: FeedRequestDTO): GlobalCachePostResponseDTO
    suspend fun getTournamentFeeds(
        tournamentId: String,
        withMetadata: Boolean,
    ): TournamentPostResponseDTO
}
