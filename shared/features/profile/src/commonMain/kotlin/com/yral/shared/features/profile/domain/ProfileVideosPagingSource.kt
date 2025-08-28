package com.yral.shared.features.profile.domain

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.core.session.SessionManager
import com.yral.shared.data.feed.domain.FeedDetails
import com.yral.shared.features.profile.domain.repository.ProfileRepository
import com.yral.shared.rust.data.models.toFeedDetails

class ProfileVideosPagingSource(
    private val profileRepository: ProfileRepository,
    private val sessionManager: SessionManager,
) : PagingSource<ULong, FeedDetails>() {
    override suspend fun load(params: LoadParams<ULong>): LoadResult<ULong, FeedDetails> =
        runCatching {
            val startIndex = params.key ?: 0UL
            val pageSize = params.loadSize.toULong()
            val result =
                profileRepository
                    .getProfileVideos(
                        startIndex = startIndex,
                        pageSize = pageSize,
                    )
            val canisterID =
                sessionManager.canisterID
                    ?: throw YralException("No canister principal found")
            val profileVideos =
                result.posts.mapNotNull { post ->
                    runCatching {
                        post.toFeedDetails(
                            postId = post.id.toLong(),
                            canisterId = canisterID,
                            nsfwProbability = 0.0, // Default value, can be updated if needed
                        )
                    }.getOrNull() // Return null for failed conversions, skip the post
                }
            LoadResult.Page(
                data = profileVideos,
                prevKey = null,
                nextKey = if (result.hasNextPage) result.nextStartIndex else null,
            )
        }.getOrElse { LoadResult.Error(it) }

    override fun getRefreshKey(state: PagingState<ULong, FeedDetails>): ULong? = null
}
