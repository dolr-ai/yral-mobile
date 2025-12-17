package com.yral.shared.features.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.yral.shared.features.chat.domain.ChatRepository
import com.yral.shared.features.chat.domain.ConversationsPagingSource
import com.yral.shared.features.chat.domain.models.Conversation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest

class ChatConversationsViewModel(
    private val chatRepository: ChatRepository,
) : ViewModel() {
    private val influencerFilter = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val conversations: Flow<PagingData<Conversation>> =
        influencerFilter
            .flatMapLatest { influencerId ->
                Pager(
                    config =
                        PagingConfig(
                            pageSize = PAGE_SIZE,
                            initialLoadSize = PAGE_SIZE,
                            prefetchDistance = PREFETCH_DISTANCE,
                            enablePlaceholders = false,
                        ),
                    pagingSourceFactory = {
                        ConversationsPagingSource(
                            chatRepository = chatRepository,
                            influencerId = influencerId,
                        )
                    },
                ).flow
            }.cachedIn(viewModelScope)

    fun setInfluencerFilter(influencerId: String?) {
        influencerFilter.value = influencerId
    }

    private companion object {
        private const val PAGE_SIZE = 20
        private const val PREFETCH_DISTANCE = 5
    }
}
