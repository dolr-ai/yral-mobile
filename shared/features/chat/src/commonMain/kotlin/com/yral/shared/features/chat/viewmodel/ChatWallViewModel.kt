package com.yral.shared.features.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.shared.analytics.events.InfluencerClickType
import com.yral.shared.analytics.events.InfluencerSource
import com.yral.shared.core.session.SessionManager
import com.yral.shared.features.chat.analytics.ChatTelemetry
import com.yral.shared.features.chat.domain.ChatErrorMapper
import com.yral.shared.features.chat.domain.ChatRepository
import com.yral.shared.features.chat.domain.InfluencersPagingSource
import com.yral.shared.features.chat.domain.models.ChatError
import com.yral.shared.features.chat.domain.models.Influencer
import com.yral.shared.features.chat.domain.usecases.GetInfluencerUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatWallViewModel(
    private val chatRepository: ChatRepository,
    private val useCaseFailureListener: UseCaseFailureListener,
    private val getInfluencerUseCase: GetInfluencerUseCase,
    private val sessionManager: SessionManager,
    private val preferences: Preferences,
    private val chatTelemetry: ChatTelemetry,
    private val chatErrorMapper: ChatErrorMapper,
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
            .observeSessionState { state ->
                val signedIn = state as? com.yral.shared.core.session.SessionState.SignedIn
                val principal = signedIn?.session?.userPrincipal
                val lastActivePrincipal = preferences.getString(PrefKeys.LAST_ACTIVE_PRINCIPAL.name)
                val activePrincipal = principal ?: lastActivePrincipal
                if (activePrincipal.isNullOrBlank()) return@observeSessionState null
                val mainPrincipal =
                    preferences.getString(PrefKeys.MAIN_PRINCIPAL.name)
                        ?: preferences.getString(PrefKeys.USER_PRINCIPAL.name)
                val isBotBySession = signedIn?.session?.isBotAccount == true
                val isBotByPrincipal = mainPrincipal != null && activePrincipal != mainPrincipal
                if (isBotBySession || isBotByPrincipal) activePrincipal else null
            }.distinctUntilChanged()
            .flatMapLatest { activeBotPrincipal ->
                // Create a new Pager when active bot changes to refresh influencers
                Pager(
                    config = pagingConfig,
                    pagingSourceFactory = { InfluencersPagingSource(chatRepository, useCaseFailureListener) },
                ).flow.map { pagingData ->
                    if (activeBotPrincipal.isNullOrBlank()) {
                        pagingData
                    } else {
                        pagingData.filter { it.id != activeBotPrincipal }
                    }
                }
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
                }.onFailure { throwable ->
                    val chatError =
                        chatErrorMapper.mapException(throwable) {
                            selectInfluencer(influencerId)
                        }
                    _state.update {
                        it.copy(
                            influencerDetail = null,
                            isInfluencerLoading = false,
                            influencerError = chatError,
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
    val influencerError: ChatError? = null,
)
