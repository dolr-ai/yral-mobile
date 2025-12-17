package com.yral.shared.features.chat.domain

import androidx.paging.PagingSource
import androidx.paging.PagingState
import co.touchlab.kermit.Logger
import com.yral.shared.features.chat.domain.models.Influencer

class InfluencersPagingSource(
    private val chatRepository: ChatRepository,
) : PagingSource<Int, Influencer>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Influencer> =
        runCatching {
            val offset = params.key ?: 0
            val limit = params.loadSize

            val result =
                chatRepository.getInfluencersPage(
                    limit = limit,
                    offset = offset,
                )

            LoadResult.Page(
                data = result.influencers,
                prevKey = null,
                nextKey = result.nextOffset,
            )
        }.getOrElse {
            Logger.e("InfluencersPaging", it)
            LoadResult.Error(it)
        }

    override fun getRefreshKey(state: PagingState<Int, Influencer>): Int? = null
}
