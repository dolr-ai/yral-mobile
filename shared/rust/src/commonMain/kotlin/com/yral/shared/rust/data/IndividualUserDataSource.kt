package com.yral.shared.rust.data

import com.yral.shared.rust.data.models.FeedRequestDTO
import com.yral.shared.rust.data.models.PostDTO
import com.yral.shared.rust.data.models.PostResponseDTO
import com.yral.shared.uniffi.generated.PostDetailsForFrontend

interface IndividualUserDataSource {
    suspend fun getInitialFeeds(feedRequestDTO: FeedRequestDTO): PostResponseDTO
    suspend fun fetchMoreFeeds(feedRequestDTO: FeedRequestDTO): PostResponseDTO
    suspend fun fetchFeedDetails(post: PostDTO): PostDetailsForFrontend
}
