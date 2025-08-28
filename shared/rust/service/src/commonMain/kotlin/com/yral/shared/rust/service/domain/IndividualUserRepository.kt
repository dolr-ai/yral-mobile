package com.yral.shared.rust.service.domain

import com.yral.shared.data.feed.domain.FeedDetails
import com.yral.shared.data.feed.domain.Post

interface IndividualUserRepository {
    suspend fun fetchFeedDetails(post: Post): FeedDetails
}
