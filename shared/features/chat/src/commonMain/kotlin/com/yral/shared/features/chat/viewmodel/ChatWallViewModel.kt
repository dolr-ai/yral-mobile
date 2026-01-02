package com.yral.shared.features.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.shared.analytics.events.InfluencerClickType
import com.yral.shared.analytics.events.InfluencerSource
import com.yral.shared.core.session.SessionManager
import com.yral.shared.core.session.SessionState
import com.yral.shared.features.chat.analytics.ChatTelemetry
import com.yral.shared.features.chat.domain.ChatRepository
import com.yral.shared.features.chat.domain.InfluencersPagingSource
import com.yral.shared.features.chat.domain.models.Influencer
import com.yral.shared.features.chat.domain.usecases.GetInfluencerUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatWallViewModel(
    private val chatRepository: ChatRepository,
    private val useCaseFailureListener: UseCaseFailureListener,
    private val getInfluencerUseCase: GetInfluencerUseCase,
    private val sessionManager: SessionManager,
    private val chatTelemetry: ChatTelemetry,
) : ViewModel() {
    private val _state = MutableStateFlow(ChatWallState())
    val state: StateFlow<ChatWallState> = _state.asStateFlow()

    private var loadInfluencerJob: Job? = null

    private val pagingConfig =
        PagingConfig(
            pageSize = PAGE_SIZE,
            initialLoadSize = PAGE_SIZE,
            prefetchDistance = PREFETCH_DISTANCE,
            enablePlaceholders = false,
        )

    fun trackInfluencerCardsViewed(influencers: List<Influencer>) {
        chatTelemetry.influencerCardsViewed(
            influencersShown = influencers.map { it.category },
            totalCards = influencers.size,
        )
    }

    fun trackInfluencerCardClicked(
        influencer: Influencer,
        position: Int,
    ) {
        chatTelemetry.influencerCardClicked(
            influencerId = influencer.id,
            influencerType = influencer.category,
            clickType = InfluencerClickType.TALK,
            position = position,
        )
        chatTelemetry.chatInfluencerClicked(
            influencerId = influencer.id,
            influencerType = influencer.category,
            source = InfluencerSource.CARD,
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val influencers: Flow<PagingData<Influencer>> =
        sessionManager
            .observeSessionPropertyWithDefault(
                selector = { it.isSocialSignIn },
                defaultValue = false,
            ).distinctUntilChanged()
            .flatMapLatest {
                // Create a new Pager when social sign-in status changes to refresh influencers
                Pager(
                    config = pagingConfig,
                    pagingSourceFactory = { InfluencersPagingSource(chatRepository, useCaseFailureListener) },
                ).flow
            }.cachedIn(viewModelScope)

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
