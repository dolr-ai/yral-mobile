package com.yral.shared.features.tournament.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.shared.core.session.SessionManager
import com.yral.shared.features.tournament.domain.GetTournamentLeaderboardUseCase
import com.yral.shared.features.tournament.domain.model.GetTournamentLeaderboardRequest
import com.yral.shared.features.tournament.domain.model.LeaderboardRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TournamentLeaderboardUiState(
    val leaderboard: List<LeaderboardRow> = emptyList(),
    val currentUser: LeaderboardRow? = null,
    val prizeMap: Map<Int, Int> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

class TournamentLeaderboardViewModel(
    private val getTournamentLeaderboardUseCase: GetTournamentLeaderboardUseCase,
    private val sessionManager: SessionManager,
) : ViewModel() {
    private val _state = MutableStateFlow(TournamentLeaderboardUiState())
    val state: StateFlow<TournamentLeaderboardUiState> = _state.asStateFlow()

    fun loadLeaderboard(tournamentId: String) {
        viewModelScope.launch {
            sessionManager.userPrincipal?.let { userPrincipal ->
                _state.update { it.copy(isLoading = true, error = null) }
                getTournamentLeaderboardUseCase
                    .invoke(GetTournamentLeaderboardRequest(principalId = userPrincipal, tournamentId = tournamentId))
                    .onSuccess { leaderboard ->
                        _state.update {
                            it.copy(
                                leaderboard = leaderboard.topRows,
                                currentUser = leaderboard.userRow,
                                prizeMap = leaderboard.prizeMap,
                                isLoading = false,
                                error = null,
                            )
                        }
                    }.onFailure { error ->
                        _state.update { it.copy(isLoading = false, error = error.message) }
                    }
            }
        }
    }

    fun isCurrentUser(principalId: String): Boolean = principalId == sessionManager.userPrincipal
}
