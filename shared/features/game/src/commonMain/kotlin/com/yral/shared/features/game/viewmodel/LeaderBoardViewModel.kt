package com.yral.shared.features.game.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.shared.core.session.SessionManager
import com.yral.shared.features.game.analytics.LeaderBoardTelemetry
import com.yral.shared.features.game.data.models.LeaderboardMode
import com.yral.shared.features.game.domain.GetLeaderboardUseCase
import com.yral.shared.features.game.domain.models.CurrentUserInfo
import com.yral.shared.features.game.domain.models.GetLeaderboardRequest
import com.yral.shared.features.game.domain.models.LeaderboardItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LeaderBoardViewModel(
    private val getLeaderboardUseCase: GetLeaderboardUseCase,
    private val sessionManager: SessionManager,
    val leaderBoardTelemetry: LeaderBoardTelemetry,
) : ViewModel() {
    private val _state = MutableStateFlow(LeaderBoardState())
    val state: StateFlow<LeaderBoardState> = _state.asStateFlow()
    private var countdownJob: Job? = null

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, countDownMs = null) }
            sessionManager.userPrincipal?.let { userPrincipal ->
                getLeaderboardUseCase
                    .invoke(
                        parameter =
                            GetLeaderboardRequest(
                                principalId = userPrincipal,
                                mode = _state.value.selectedMode,
                            ),
                    ).onSuccess { data ->
                        _state.update {
                            it.copy(
                                leaderboard = data.topRows,
                                currentUser = data.userRow.toCurrentUserInfo(),
                                isLoading = false,
                                countDownMs = data.timeLeftMs,
                            )
                        }
                        startCountDown()
                    }.onFailure { error ->
                        _state.update {
                            it.copy(
                                error = "Failed to load leaderboard: ${error.message}",
                                isLoading = false,
                            )
                        }
                    }
            }
        }
    }

    @Suppress("MagicNumber")
    private fun startCountDown() {
        countdownJob?.cancel()
        countdownJob =
            viewModelScope.launch {
                while ((_state.value.countDownMs ?: 0) > 0) {
                    delay(1000L)
                    _state.update {
                        val newTime = ((_state.value.countDownMs ?: 0) - 1000L).coerceAtLeast(0L)
                        it.copy(countDownMs = if (newTime == 0L) null else newTime)
                    }
                }
            }
    }

    private fun refreshData() {
        _state.update { it.copy(error = null) }
        loadData()
    }

    fun selectMode(mode: LeaderboardMode) {
        if (_state.value.selectedMode != mode) {
            _state.update { it.copy(selectedMode = mode) }
            refreshData()
        }
    }
}

fun LeaderboardItem.toCurrentUserInfo(): CurrentUserInfo =
    CurrentUserInfo(
        userPrincipalId = userPrincipalId,
        profileImageUrl = profileImage,
        wins = wins,
        leaderboardPosition = position,
    )

data class LeaderBoardState(
    val leaderboard: List<LeaderboardItem> = emptyList(),
    val currentUser: CurrentUserInfo? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedMode: LeaderboardMode = LeaderboardMode.DAILY,
    val countDownMs: Long? = null,
)
