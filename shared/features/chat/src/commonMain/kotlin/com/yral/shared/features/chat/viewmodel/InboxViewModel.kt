package com.yral.shared.features.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.yral.shared.core.session.SessionManager
import com.yral.shared.features.chat.domain.ChatRepository
import com.yral.shared.features.chat.domain.ConversationsPagingSource
import com.yral.shared.features.chat.domain.models.Conversation
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 20
private const val PREFETCH_DISTANCE = 10

class InboxViewModel(
    private val chatRepository: ChatRepository,
    private val sessionManager: SessionManager,
    private val useCaseFailureListener: UseCaseFailureListener,
    private val chatUnreadRefreshSignal: ChatUnreadRefreshSignal,
) : ViewModel() {
    private val refreshTrigger = MutableStateFlow(0)
    private val mutableUnreadConversationCount = MutableStateFlow(0)

    private val pagingConfig =
        PagingConfig(
            pageSize = PAGE_SIZE,
            initialLoadSize = PAGE_SIZE,
            prefetchDistance = PREFETCH_DISTANCE,
            enablePlaceholders = false,
        )

    val unreadConversationCount = mutableUnreadConversationCount.asStateFlow()

    init {
        refreshUnreadConversationCount()
        chatUnreadRefreshSignal.requests
            .onEach { refreshConversations() }
            .launchIn(viewModelScope)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val conversations: Flow<PagingData<Conversation>> =
        refreshTrigger
            .flatMapLatest {
                val principal =
                    sessionManager.userPrincipal
                        ?: return@flatMapLatest flowOf(PagingData.empty())
                Pager(
                    config = pagingConfig,
                    pagingSourceFactory = {
                        ConversationsPagingSource(
                            chatRepository = chatRepository,
                            useCaseFailureListener = useCaseFailureListener,
                            influencerId = null,
                            principal = principal,
                        )
                    },
                ).flow
            }.cachedIn(viewModelScope)

    fun refreshConversations() {
        refreshTrigger.update { it + 1 }
        refreshUnreadConversationCount()
    }

    private fun refreshUnreadConversationCount() {
        viewModelScope.launch {
            val principal = sessionManager.userPrincipal
            if (principal == null) {
                mutableUnreadConversationCount.value = 0
                return@launch
            }

            runCatching {
                chatRepository.getUnreadConversationCount(principal = principal)
            }.onSuccess { unreadCount ->
                mutableUnreadConversationCount.value = unreadCount
            }
        }
    }
}
