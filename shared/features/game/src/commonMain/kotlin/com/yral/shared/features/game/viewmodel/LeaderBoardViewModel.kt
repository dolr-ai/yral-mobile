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

    fun loadData() {
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
                                blinkCountDown =
                                    data.timeLeftMs?.let { timeLeft ->
                                        timeLeft < COUNT_DOWN_BLINK_THRESHOLD
                                    } == true,
                            )
                        }
                        data.timeLeftMs?.let { startCountDown() }
                    }.onFailure { error ->
                        _state.update { it.copy(error = error.message, isLoading = false) }
                    }
            }
        }
    }

    @Suppress("MagicNumber")
    private fun startCountDown() {
        countdownJob?.cancel()
        countdownJob =
            viewModelScope.launch {
                var currentTime = _state.value.countDownMs
                while (currentTime != null && currentTime > 0) {
                    delay(1000L)
                    currentTime = (currentTime - 1000L).coerceAtLeast(0L)
                    _state.update {
                        it.copy(
                            countDownMs = if (currentTime == 0L) null else currentTime,
                            blinkCountDown = currentTime < COUNT_DOWN_BLINK_THRESHOLD,
                        )
                    }
                    currentTime = _state.value.countDownMs
                }
                refreshData()
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

    companion object {
        private const val COUNT_DOWN_BLINK_THRESHOLD = 2 * 60 * 60 * 1000 // Last 2 hours
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
    val blinkCountDown: Boolean = false,
)
