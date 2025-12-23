package com.yral.shared.features.chat.domain

import androidx.paging.PagingSource
import androidx.paging.PagingState
import co.touchlab.kermit.Logger
import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.features.chat.domain.models.ChatMessage
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import kotlinx.coroutines.CancellationException

class ConversationMessagesPagingSource(
    private val conversationId: String,
    private val chatRepository: ChatRepository,
    private val useCaseFailureListener: UseCaseFailureListener,
    private val initialOffset: Int? = null,
) : PagingSource<Int, ChatMessage>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ChatMessage> =
        try {
            val offset = params.key ?: initialOffset ?: 0
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
        } catch (e: CancellationException) {
            throw e
        } catch (
            @Suppress("TooGenericExceptionCaught")
            e: Exception,
        ) {
            Logger.e("ConversationMessagesPaging", e)
            useCaseFailureListener.onFailure(
                e,
                tag = this::class.simpleName!!,
                message = { "failed to fetch conversation messages" },
                exceptionType = ExceptionType.CHAT.name,
            )
            LoadResult.Error(e)
        }

    override fun getRefreshKey(state: PagingState<Int, ChatMessage>): Int? = null
}
