package com.yral.shared.rust.domain

import com.yral.shared.rust.domain.models.FeedDetails
import com.yral.shared.rust.domain.models.FeedRequest
import com.yral.shared.rust.domain.models.Post
import com.yral.shared.rust.domain.models.PostResponse
import com.yral.shared.uniffi.generated.VideoGenRequestKey
import com.yral.shared.uniffi.generated.VideoGenRequestStatus

interface IndividualUserRepository {
    suspend fun getInitialFeeds(feedRequest: FeedRequest): PostResponse
    suspend fun fetchMoreFeeds(feedRequest: FeedRequest): PostResponse
    suspend fun fetchFeedDetails(post: Post): FeedDetails
    suspend fun fetchVideoGenerationStatus(requestKey: VideoGenRequestKey): VideoGenRequestStatus
}
