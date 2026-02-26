package com.yral.shared.features.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.github.michaelbull.result.onSuccess
import com.yral.shared.core.session.SessionManager
import com.yral.shared.features.chat.domain.ChatRepository
import com.yral.shared.features.chat.domain.ConversationsPagingSource
import com.yral.shared.features.chat.domain.models.Conversation
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.rust.service.domain.models.UserProfileDetails
import com.yral.shared.rust.service.domain.usecases.GetUsersProfileDetailsParams
import com.yral.shared.rust.service.domain.usecases.GetUsersProfileDetailsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 20
private const val PREFETCH_DISTANCE = 10

class InboxViewModel(
    private val chatRepository: ChatRepository,
    private val sessionManager: SessionManager,
    private val getUsersProfileDetailsUseCase: GetUsersProfileDetailsUseCase,
    private val useCaseFailureListener: UseCaseFailureListener,
) : ViewModel() {
    private val _state = MutableStateFlow(InboxState())
    val state: StateFlow<InboxState> = _state.asStateFlow()

    private val refreshTrigger = MutableStateFlow(0)

    private val pageLoadedChannel = Channel<List<Conversation>>(Channel.UNLIMITED)

    private val pagingConfig =
        PagingConfig(
            pageSize = PAGE_SIZE,
            initialLoadSize = PAGE_SIZE,
            prefetchDistance = PREFETCH_DISTANCE,
            enablePlaceholders = false,
        )

    init {
        viewModelScope.launch {
            pageLoadedChannel.receiveAsFlow().collect { conversations ->
                if (sessionManager.isBotAccount == true) {
                    val userIds = conversations.map { it.userId }.distinct()
                    loadProfileDetailsForUserIds(userIds)
                }
            }
        }
        viewModelScope.launch {
            sessionManager
                .observeSessionState { it }
                .collect { _state.update { it.copy(isBotAccount = sessionManager.isBotAccount == true) } }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val conversations: Flow<PagingData<Conversation>> =
        refreshTrigger
            .flatMapLatest {
                Pager(
                    config = pagingConfig,
                    pagingSourceFactory = {
                        ConversationsPagingSource(
                            chatRepository = chatRepository,
                            useCaseFailureListener = useCaseFailureListener,
                            influencerId = null,
                            onPageLoaded = { page -> pageLoadedChannel.trySend(page) },
                        )
                    },
                ).flow
            }.cachedIn(viewModelScope)

    fun refreshConversations() {
        refreshTrigger.update { it + 1 }
    }

    fun loadProfileDetailsForUserIds(userIds: List<String>) {
        val callerPrincipal = sessionManager.userPrincipal
        if (
            sessionManager.isBotAccount != true ||
            userIds.isEmpty() ||
            callerPrincipal == null
        ) {
            return
        }
        val alreadyLoaded = _state.value.profileDetailsByUserId.map { it.key }
        val toLoad =
            userIds
                .filter { it.isNotBlank() }
                .distinct()
                .filter { it !in alreadyLoaded }
        if (toLoad.isEmpty()) return
        viewModelScope.launch {
            getUsersProfileDetailsUseCase(
                GetUsersProfileDetailsParams(
                    callerPrincipal = callerPrincipal,
                    targetPrincipalIds = toLoad,
                ),
            ).onSuccess { newMap ->
                _state.update { current ->
                    current.copy(
                        profileDetailsByUserId = current.profileDetailsByUserId + newMap.mapKeys { it.key },
                    )
                }
            }
        }
    }
}

data class InboxState(
    val isLoading: Boolean = false,
    val isBotAccount: Boolean = false,
    val profileDetailsByUserId: Map<String, UserProfileDetails> = emptyMap(),
)
