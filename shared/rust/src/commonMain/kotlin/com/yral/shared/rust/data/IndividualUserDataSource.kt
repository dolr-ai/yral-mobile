package com.yral.shared.rust.data

import com.yral.shared.rust.data.models.FeedRequestDTO
import com.yral.shared.rust.data.models.PostResponseDTO

interface IndividualUserDataSource {
    suspend fun getInitialFeeds(feedRequestDTO: FeedRequestDTO): PostResponseDTO
    suspend fun fetchMoreFeeds(feedRequestDTO: FeedRequestDTO): PostResponseDTO
}
