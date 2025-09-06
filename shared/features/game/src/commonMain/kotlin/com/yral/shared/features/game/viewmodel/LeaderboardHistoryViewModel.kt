package com.yral.shared.features.game.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.shared.core.session.SessionManager
import com.yral.shared.features.game.domain.GetLeaderboardHistoryUseCase
import com.yral.shared.features.game.domain.models.LeaderboardHistory
import com.yral.shared.features.game.domain.models.LeaderboardHistoryRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LeaderboardHistoryViewModel(
    private val getLeaderboardHistoryUseCase: GetLeaderboardHistoryUseCase,
    private val sessionManager: SessionManager,
) : ViewModel() {
    private val _state = MutableStateFlow(LeaderboardHistoryState())
    val state: StateFlow<LeaderboardHistoryState> = _state.asStateFlow()

    init {
        fetchHistory()
    }

    fun fetchHistory() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val principal = sessionManager.userPrincipal
            if (principal == null) {
                _state.update { it.copy(isLoading = false, error = "") }
                return@launch
            }
            getLeaderboardHistoryUseCase
                .invoke(LeaderboardHistoryRequest(principalId = principal))
                .onSuccess { history ->
                    _state.update {
                        it.copy(isLoading = false, history = history, selectedIndex = 0)
                    }
                }.onFailure { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun select(index: Int) {
        _state.update { it.copy(selectedIndex = index) }
    }
}

data class LeaderboardHistoryState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val history: LeaderboardHistory = emptyList(),
    val selectedIndex: Int = 0,
)
