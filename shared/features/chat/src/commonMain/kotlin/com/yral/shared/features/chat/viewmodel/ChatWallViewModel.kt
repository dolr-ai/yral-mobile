package com.yral.shared.features.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.shared.features.chat.domain.ChatRepository
import com.yral.shared.features.chat.domain.InfluencersPagingSource
import com.yral.shared.features.chat.domain.models.Influencer
import com.yral.shared.features.chat.domain.usecases.GetInfluencerUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatWallViewModel(
    private val chatRepository: ChatRepository,
    private val useCaseFailureListener: UseCaseFailureListener,
    private val getInfluencerUseCase: GetInfluencerUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(ChatWallState())
    val state: StateFlow<ChatWallState> = _state.asStateFlow()

    private var loadInfluencerJob: Job? = null

    val influencers: Flow<PagingData<Influencer>> =
        Pager(
            config =
                PagingConfig(
                    pageSize = PAGE_SIZE,
                    initialLoadSize = PAGE_SIZE,
                    prefetchDistance = PREFETCH_DISTANCE,
                    enablePlaceholders = false,
                ),
            pagingSourceFactory = { InfluencersPagingSource(chatRepository, useCaseFailureListener) },
        ).flow.cachedIn(viewModelScope)

    fun selectInfluencer(influencerId: String) {
        loadInfluencerJob?.cancel()
        _state.update {
            it.copy(
                selectedInfluencerId = influencerId,
                influencerDetail = null,
                isInfluencerLoading = true,
                influencerError = null,
            )
        }
        loadInfluencerJob =
            viewModelScope.launch {
                getInfluencerUseCase(
                    GetInfluencerUseCase.Params(id = influencerId),
                ).onSuccess { influencer ->
                    _state.update {
                        it.copy(
                            influencerDetail = influencer,
                            isInfluencerLoading = false,
                            influencerError = null,
                        )
                    }
                }.onFailure { error ->
                    _state.update {
                        it.copy(
                            influencerDetail = null,
                            isInfluencerLoading = false,
                            influencerError = error.message ?: "Failed to load influencer",
                        )
                    }
                }
            }
    }

    fun clearSelection() {
        loadInfluencerJob?.cancel()
        _state.update {
            it.copy(
                selectedInfluencerId = null,
                influencerDetail = null,
                isInfluencerLoading = false,
                influencerError = null,
            )
        }
    }

    private companion object {
        private const val PAGE_SIZE = 50
        private const val PREFETCH_DISTANCE = 10
    }
}

data class ChatWallState(
    val selectedInfluencerId: String? = null,
    val influencerDetail: Influencer? = null,
    val isInfluencerLoading: Boolean = false,
    val influencerError: String? = null,
)
