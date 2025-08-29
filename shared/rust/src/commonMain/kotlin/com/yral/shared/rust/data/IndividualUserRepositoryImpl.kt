package com.yral.shared.rust.data

import com.yral.shared.data.feed.domain.FeedDetails
import com.yral.shared.data.feed.domain.Post
import com.yral.shared.data.feed.domain.toDTO
import com.yral.shared.rust.data.models.Posts
import com.yral.shared.rust.data.models.PostsOfUserProfileError
import com.yral.shared.rust.data.models.toFeedDetails
import com.yral.shared.rust.data.models.toPosts
import com.yral.shared.rust.domain.IndividualUserRepository

class IndividualUserRepositoryImpl(
    private val dataSource: IndividualUserDataSource,
) : IndividualUserRepository {
    override suspend fun fetchFeedDetails(
        post: Post,
        shouldFetchFromServiceCanisters: Boolean,
    ): FeedDetails =
        if (!shouldFetchFromServiceCanisters) {
            dataSource
                .fetchFeedDetails(post.toDTO())
                .toFeedDetails(
                    postId = post.postID,
                    canisterId = post.canisterID,
                    nsfwProbability = post.nsfwProbability,
                )
        } else {
            dataSource
                .fetchSCFeedDetails(post.toDTO())
                .toFeedDetails(
                    postId = post.postID,
                    canisterId = post.canisterID,
                    nsfwProbability = post.nsfwProbability,
                )
        }

    override suspend fun getPostsOfThisUserProfileWithPaginationCursor(
        principalId: String,
        startIndex: ULong,
        pageSize: ULong,
        shouldFetchFromServiceCanisters: Boolean,
    ): Posts {
        return if (!shouldFetchFromServiceCanisters) {
            dataSource
                .getPostsOfThisUserProfileWithPaginationCursor(
                    principalId = principalId,
                    startIndex = startIndex,
                    pageSize = pageSize,
                ).toPosts(principalId)
        } else {
            val posts =
                dataSource
                    .getSCPostsOfThisUserProfileWithPaginationCursor(
                        principalId = principalId,
                        startIndex = startIndex,
                        pageSize = pageSize,
                    )
            when (posts.isEmpty()) {
                true -> return Posts.Err(PostsOfUserProfileError.REACHED_END_OF_ITEMS_LIST)
                false -> {
                    Posts.Ok(
                        v1 =
                            posts.map {
                                runCatching {
                                    it.toFeedDetails(
                                        postId = it.id.toLong(),
                                        canisterId = principalId,
                                        nsfwProbability = 0.0,
                                    )
                                }.getOrNull()
                            },
                    )
                }
            }
        }
    }
}
