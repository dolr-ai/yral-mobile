package com.yral.shared.features.game.viewmodel

import androidx.lifecycle.ViewModel
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.core.session.SessionManager
import com.yral.shared.features.game.analytics.LeaderBoardTelemetry
import com.yral.shared.features.game.domain.GetCurrentUserInfoUseCase
import com.yral.shared.features.game.domain.GetLeaderboardUseCase
import com.yral.shared.features.game.domain.models.CurrentUserInfo
import com.yral.shared.features.game.domain.models.LeaderboardItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LeaderBoardViewModel(
    appDispatchers: AppDispatchers,
    private val getLeaderboardUseCase: GetLeaderboardUseCase,
    private val getCurrentUserInfoUseCase: GetCurrentUserInfoUseCase,
    private val sessionManager: SessionManager,
    val leaderBoardTelemetry: LeaderBoardTelemetry,
) : ViewModel() {
    private val coroutineScope = CoroutineScope(SupervisorJob() + appDispatchers.io)

    private val _state = MutableStateFlow(LeaderBoardState())
    val state: StateFlow<LeaderBoardState> = _state.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        coroutineScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            sessionManager.userPrincipal?.let { userPrincipal ->
                listOf(
                    async { fetchLeaderBoard() },
                    async { fetchCurrentUserInfo(userPrincipal) },
                ).awaitAll()
            }
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun refreshData() {
        _state.update { currentState ->
            currentState.copy(error = null)
        }
        loadData()
    }

    private suspend fun fetchLeaderBoard() {
        getLeaderboardUseCase
            .invoke(Unit)
            .onSuccess { leaderboard ->
                _state.update { currentState ->
                    currentState.copy(leaderboard = leaderboard)
                }
            }.onFailure { error ->
                _state.update { it.copy(error = "Failed to load leaderboard: ${error.message}") }
            }
    }

    private suspend fun fetchCurrentUserInfo(userPrincipalId: String) {
        getCurrentUserInfoUseCase
            .invoke(GetCurrentUserInfoUseCase.Params(userPrincipalId))
            .onSuccess { userInfo ->
                _state.update { currentState ->
                    currentState.copy(currentUser = userInfo)
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(error = "Failed to load current user info: ${error.message}")
                }
            }
    }

    fun updateCurrentUserRank(newRank: Int) {
        _state.update { currentState ->
            currentState.copy(
                currentUser = currentState.currentUser?.copy(rank = newRank),
            )
        }
    }
}

data class LeaderBoardState(
    val leaderboard: List<LeaderboardItem> = emptyList(),
    val currentUser: CurrentUserInfo? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)
