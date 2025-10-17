package com.yral.shared.features.leaderboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.shared.core.session.SessionManager
import com.yral.shared.features.leaderboard.analytics.LeaderBoardTelemetry
import com.yral.shared.features.leaderboard.data.models.LeaderboardMode
import com.yral.shared.features.leaderboard.domain.GetLeaderboardRankForTodayUseCase
import com.yral.shared.features.leaderboard.domain.GetLeaderboardUseCase
import com.yral.shared.features.leaderboard.domain.models.GetLeaderboardRequest
import com.yral.shared.features.leaderboard.domain.models.LeaderboardDailyRankRequest
import com.yral.shared.features.leaderboard.domain.models.LeaderboardItem
import com.yral.shared.features.leaderboard.domain.models.RewardCurrency
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LeaderBoardViewModel(
    private val getLeaderboardUseCase: GetLeaderboardUseCase,
    private val getLeaderboardRankForTodayUseCase: GetLeaderboardRankForTodayUseCase,
    private val sessionManager: SessionManager,
    private val leaderBoardTelemetry: LeaderBoardTelemetry,
) : ViewModel() {
    private val _state = MutableStateFlow(LeaderBoardState())
    val state: StateFlow<LeaderBoardState> = _state.asStateFlow()
    private var countdownJob: Job? = null

    val dailyRank = sessionManager.observeSessionProperty { it.dailyRank }

    val refreshRank =
        sessionManager
            .observeSessionProperty { it.isFirebaseLoggedIn }
            .onEach { isLoggedIn -> if (isLoggedIn) refreshTodayRank() }

    init {
        viewModelScope.launch {
            sessionManager
                .observeSessionProperty { it.isFirebaseLoggedIn }
                .collect { isFirebaseLoggedIn ->
                    _state.update { it.copy(isFirebaseLoggedIn = isFirebaseLoggedIn) }
                }
        }
    }

    private fun resetState() {
        _state.update {
            it.copy(
                isLoading = true,
                error = null,
                countDownMs = null,
                rewardCurrency = null,
                rewardCurrencyCode = null,
                rewardsTable = null,
            )
        }
    }

    fun loadData(countryCode: String) {
        viewModelScope.launch {
            resetState()
            sessionManager.userPrincipal?.let { userPrincipal ->
                getLeaderboardUseCase
                    .invoke(
                        parameter =
                            GetLeaderboardRequest(
                                principalId = userPrincipal,
                                mode = _state.value.selectedMode,
                                countryCode = countryCode,
                            ),
                    ).onSuccess { data ->
                        val currentUser =
                            data.userRow
                                ?: data.topRows.firstOrNull { item ->
                                    item.userPrincipalId == sessionManager.userPrincipal
                                }
                        _state.update {
                            it.copy(
                                leaderboard = data.topRows,
                                currentUser = data.userRow,
                                isCurrentUserInTop = (currentUser?.position ?: Int.MAX_VALUE) <= TOP_N_THRESHOLD,
                                isLoading = false,
                                countDownMs = data.timeLeftMs,
                                blinkCountDown =
                                    data.timeLeftMs?.let { timeLeft ->
                                        timeLeft < COUNT_DOWN_BLINK_THRESHOLD
                                    } == true,
                                rewardCurrency = data.rewardCurrency,
                                rewardCurrencyCode = data.rewardCurrencyCode,
                                rewardsTable = data.rewardsTable,
                            )
                        }
                        data.timeLeftMs?.let { startCountDown(countryCode) }
                    }.onFailure { error ->
                        // No error to be shown on UI setting error as null
                        _state.update { it.copy(error = null, isLoading = false) }
                    }
            }
        }
    }

    @Suppress("MagicNumber")
    private fun startCountDown(countryCode: String) {
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
                refreshData(countryCode)
            }
    }

    private fun refreshData(countryCode: String) {
        _state.update { it.copy(error = null) }
        loadData(countryCode)
    }

    fun selectMode(
        mode: LeaderboardMode,
        countryCode: String,
    ) {
        if (_state.value.selectedMode != mode) {
            _state.update { it.copy(selectedMode = mode) }
            refreshData(countryCode)
            leaderBoardTelemetry.leaderboardTabClicked(mode)
        }
    }

    fun isCurrentUser(principal: String) = principal == sessionManager.userPrincipal

    fun leaderboardPageViewed() {
        leaderBoardTelemetry.leaderboardPageViewed(state.value.selectedMode)
    }

    fun leaderboardCalendarClicked() {
        with(_state.value) {
            val currentUser =
                currentUser ?: leaderboard.firstOrNull { item -> item.userPrincipalId == sessionManager.userPrincipal }
            leaderBoardTelemetry.leaderboardCalendarClicked(
                tab = selectedMode,
                rank = currentUser?.position ?: Int.MAX_VALUE,
            )
        }
    }

    fun reportLeaderboardPageLoaded(visibleRows: Int) {
        with(_state.value) {
            val currentUser =
                currentUser ?: leaderboard.firstOrNull { item -> item.userPrincipalId == sessionManager.userPrincipal }
            leaderBoardTelemetry.leaderboardPageLoaded(
                tab = selectedMode,
                rank = currentUser?.position ?: Int.MAX_VALUE,
                visibleRows = visibleRows,
            )
        }
    }

    private suspend fun refreshTodayRank() {
        Logger.d("DailyRank") { "Fetching today rank" }
        sessionManager.userPrincipal?.let { userPrincipal ->
            getLeaderboardRankForTodayUseCase(
                parameter = LeaderboardDailyRankRequest(userPrincipal),
            ).onSuccess {
                Logger.d("DailyRank") { "rank in vm $it" }
                sessionManager.updateDailyRank(it.position)
            }.onFailure {
                Logger.d("DailyRank") { "rank fetching error $it" }
                sessionManager.updateDailyRank(null)
            }
        }
    }

    companion object {
        private const val COUNT_DOWN_BLINK_THRESHOLD = 2 * 60 * 60 * 1000 // Last 2 hours
        const val TOP_N_THRESHOLD = 3
    }
}

data class LeaderBoardState(
    val leaderboard: List<LeaderboardItem> = emptyList(),
    val currentUser: LeaderboardItem? = null,
    val isCurrentUserInTop: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedMode: LeaderboardMode = LeaderboardMode.DAILY,
    val countDownMs: Long? = null,
    val blinkCountDown: Boolean = false,
    val rewardCurrency: RewardCurrency? = null,
    val rewardCurrencyCode: String? = null,
    val rewardsTable: Map<Int, Double>? = null,
    val isFirebaseLoggedIn: Boolean = false,
)
