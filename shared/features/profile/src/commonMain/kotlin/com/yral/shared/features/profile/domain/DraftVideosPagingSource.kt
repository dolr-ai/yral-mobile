package com.yral.shared.features.profile.domain

import androidx.paging.PagingSource
import androidx.paging.PagingState
import co.touchlab.kermit.Logger
import com.yral.shared.data.domain.models.FeedDetails
import com.yral.shared.features.profile.domain.repository.ProfileRepository

class DraftVideosPagingSource(
    private val canisterId: String,
    private val profileRepository: ProfileRepository,
) : PagingSource<ULong, FeedDetails>() {
    override suspend fun load(params: LoadParams<ULong>): LoadResult<ULong, FeedDetails> =
        runCatching {
            val startIndex = params.key ?: 0UL
            val pageSize = params.loadSize.toULong()

            val result =
                profileRepository.getDraftVideos(
                    canisterId = canisterId,
                    startIndex = startIndex,
                    pageSize = pageSize,
                )
            LoadResult.Page(
                data = result.posts,
                prevKey = null,
                nextKey = if (result.hasNextPage) result.nextStartIndex else null,
            )
        }.getOrElse {
            Logger.e("DraftVideosPaging", it)
            LoadResult.Error(it)
        }

    override fun getRefreshKey(state: PagingState<ULong, FeedDetails>): ULong? = null
}
