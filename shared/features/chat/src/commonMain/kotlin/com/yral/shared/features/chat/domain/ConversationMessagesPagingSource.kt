package com.yral.shared.features.chat.domain

import androidx.paging.PagingSource
import androidx.paging.PagingState
import co.touchlab.kermit.Logger
import com.yral.shared.features.chat.domain.models.ChatMessage

class ConversationMessagesPagingSource(
    private val conversationId: String,
    private val chatRepository: ChatRepository,
) : PagingSource<Int, ChatMessage>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ChatMessage> =
        runCatching {
            val offset = params.key ?: 0
            val limit = params.loadSize

            val result =
                chatRepository.getConversationMessagesPage(
                    conversationId = conversationId,
                    limit = limit,
                    offset = offset,
                )

            LoadResult.Page(
                data = result.messages,
                prevKey = null,
                nextKey = result.nextOffset,
            )
        }.getOrElse {
            Logger.e("ConversationMessagesPaging", it)
            LoadResult.Error(it)
        }

    override fun getRefreshKey(state: PagingState<Int, ChatMessage>): Int? = null
}
