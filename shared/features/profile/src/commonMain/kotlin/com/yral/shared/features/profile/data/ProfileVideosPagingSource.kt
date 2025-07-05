package com.yral.shared.features.profile.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.yral.shared.core.session.SessionManager
import com.yral.shared.features.profile.domain.repository.ProfileRepository
import com.yral.shared.rust.data.models.toFeedDetails
import com.yral.shared.rust.domain.models.FeedDetails

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
            val canisterId = sessionManager.getCanisterPrincipal() ?: ""
            val profileVideos =
                result.posts.mapNotNull { post ->
                    runCatching {
                        post.toFeedDetails(
                            postId = post.id.toLong(),
                            canisterId = canisterId,
                            nsfwProbability = 0.0, // Default value, can be updated if needed
                        )
                    }.getOrNull() // Return null for failed conversions, skip the post
                }
            LoadResult.Page(
                data = profileVideos,
                prevKey = if (startIndex >= pageSize) startIndex - pageSize else null,
                nextKey = if (result.hasNextPage) result.nextStartIndex else null,
            )
        }.getOrElse { LoadResult.Error(it) }

    override fun getRefreshKey(state: PagingState<ULong, FeedDetails>): ULong? = null
}
