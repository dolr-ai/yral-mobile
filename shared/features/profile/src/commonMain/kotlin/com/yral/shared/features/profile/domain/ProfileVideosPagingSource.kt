package com.yral.shared.features.profile.domain

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.yral.shared.data.feed.domain.FeedDetails
import com.yral.shared.features.profile.domain.repository.ProfileRepository

class ProfileVideosPagingSource(
    private val canisterId: String,
    private val userPrincipal: String,
    private val isFromServiceCanister: Boolean,
    private val profileRepository: ProfileRepository,
) : PagingSource<ULong, FeedDetails>() {
    override suspend fun load(params: LoadParams<ULong>): LoadResult<ULong, FeedDetails> =
        runCatching {
            val startIndex = params.key ?: 0UL
            val pageSize = params.loadSize.toULong()
            val result =
                profileRepository
                    .getProfileVideos(
                        canisterId = canisterId,
                        userPrincipal = userPrincipal,
                        isFromServiceCanister = isFromServiceCanister,
                        startIndex = startIndex,
                        pageSize = pageSize,
                    )
            val profileVideos = result.posts
            LoadResult.Page(
                data = profileVideos,
                prevKey = null,
                nextKey = if (result.hasNextPage) result.nextStartIndex else null,
            )
        }.getOrElse { LoadResult.Error(it) }

    override fun getRefreshKey(state: PagingState<ULong, FeedDetails>): ULong? = null
}
