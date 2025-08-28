package com.yral.shared.rust.service.data

import com.yral.shared.data.feed.domain.FeedDetails
import com.yral.shared.data.feed.domain.Post
import com.yral.shared.data.feed.domain.toDTO
import com.yral.shared.rust.service.data.models.toFeedDetails
import com.yral.shared.rust.service.domain.IndividualUserRepository

class IndividualUserRepositoryImpl(
    private val dataSource: IndividualUserDataSource,
) : IndividualUserRepository {
    override suspend fun fetchFeedDetails(post: Post): FeedDetails =
        dataSource
            .fetchFeedDetails(post.toDTO())
            .toFeedDetails(
                postId = post.postID,
                canisterId = post.canisterID,
                nsfwProbability = post.nsfwProbability,
            )
}
