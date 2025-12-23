package com.yral.shared.features.chat.domain

import androidx.paging.PagingSource
import androidx.paging.PagingState
import co.touchlab.kermit.Logger
import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.features.chat.domain.models.Conversation
import com.yral.shared.libs.arch.domain.UseCaseFailureListener

class ConversationsPagingSource(
    private val chatRepository: ChatRepository,
    private val useCaseFailureListener: UseCaseFailureListener,
    private val influencerId: String?,
) : PagingSource<Int, Conversation>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Conversation> =
        runCatching {
            val offset = params.key ?: 0
            val limit = params.loadSize

            val result =
                chatRepository.getConversationsPage(
                    limit = limit,
                    offset = offset,
                    influencerId = influencerId,
                )

            LoadResult.Page(
                data = result.conversations,
                prevKey = null,
                nextKey = result.nextOffset,
            )
        }.getOrElse {
            Logger.e("ConversationsPaging", it)
            useCaseFailureListener.onFailure(
                it,
                tag = this::class.simpleName!!,
                message = { "failed to fetch influencers" },
                exceptionType = ExceptionType.CHAT.name,
            )
            LoadResult.Error(it)
        }

    override fun getRefreshKey(state: PagingState<Int, Conversation>): Int? = null
}
