package com.yral.shared.features.tournament.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.shared.core.session.SessionManager
import com.yral.shared.features.tournament.analytics.TournamentTelemetry
import com.yral.shared.features.tournament.domain.GetTournamentLeaderboardUseCase
import com.yral.shared.features.tournament.domain.model.GetTournamentLeaderboardRequest
import com.yral.shared.features.tournament.domain.model.LeaderboardRow
import com.yral.shared.features.tournament.domain.model.formatParticipantsLabel
import com.yral.shared.features.tournament.domain.model.formatScheduleLabel
import com.yral.shared.features.tournament.domain.model.tournamentStatus
import com.yral.shared.rust.service.domain.usecases.GetProfileDetailsV4Params
import com.yral.shared.rust.service.domain.usecases.GetProfileDetailsV4UseCase
import com.yral.shared.rust.service.utils.CanisterData
import com.yral.shared.rust.service.utils.getUserInfoServiceCanister
import com.yral.shared.rust.service.utils.propicFromPrincipal
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class TournamentLeaderboardUiState(
    val leaderboard: List<LeaderboardRow> = emptyList(),
    val currentUser: LeaderboardRow? = null,
    val prizeMap: Map<Int, Int> = emptyMap(),
    val endEpochMs: Long = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isNavigating: Boolean = false,
    val participantsLabel: String? = null,
    val scheduleLabel: String? = null,
    val showResultOverlay: Boolean = false,
)

class TournamentLeaderboardViewModel(
    private val getTournamentLeaderboardUseCase: GetTournamentLeaderboardUseCase,
    private val sessionManager: SessionManager,
    private val getProfileDetailsV4UseCase: GetProfileDetailsV4UseCase,
    private val telemetry: TournamentTelemetry,
) : ViewModel() {
    private val _state = MutableStateFlow(TournamentLeaderboardUiState())
    val state: StateFlow<TournamentLeaderboardUiState> = _state.asStateFlow()
    private val eventChannel = Channel<Event>(Channel.BUFFERED)
    val eventsFlow: Flow<Event> = eventChannel.receiveAsFlow()

    @OptIn(ExperimentalTime::class)
    fun loadLeaderboard(tournamentId: String) {
        viewModelScope.launch {
            sessionManager.userPrincipal?.let { userPrincipal ->
                _state.update { it.copy(isLoading = true, error = null) }
                getTournamentLeaderboardUseCase
                    .invoke(GetTournamentLeaderboardRequest(principalId = userPrincipal, tournamentId = tournamentId))
                    .onSuccess { leaderboard ->
                        // Track leaderboard viewed
                        val userRank = leaderboard.userRow?.position ?: 0
                        val userPrize = leaderboard.userRow?.prize ?: leaderboard.prizeMap[userRank]
                        val isWinner = userRank > 0 && userPrize != null
                        telemetry.onLeaderboardViewed(
                            tournamentId = tournamentId,
                            userRank = userRank,
                            isWinner = isWinner,
                        )

                        // Track reward earned if user won a prize
                        if (isWinner) {
                            telemetry.onRewardEarned(
                                tournamentId = tournamentId,
                                rewardAmountInr = userPrize ?: 0,
                                rank = userRank,
                            )
                        }

                        _state.update {
                            val startTime = Instant.fromEpochMilliseconds(leaderboard.startEpochMs)
                            val endTime = Instant.fromEpochMilliseconds(leaderboard.endEpochMs)
                            val currentTime = Clock.System.now()
                            val scheduleLabel = formatScheduleLabel(leaderboard.date, startTime, endTime)
                            val tournamentStatus = tournamentStatus(currentTime, startTime, endTime)
                            val participantsLabel =
                                formatParticipantsLabel(
                                    leaderboard.participantCount,
                                    tournamentStatus,
                                )
                            it.copy(
                                leaderboard = leaderboard.topRows,
                                currentUser = leaderboard.userRow,
                                prizeMap = leaderboard.prizeMap,
                                endEpochMs = leaderboard.endEpochMs,
                                isLoading = false,
                                error = null,
                                scheduleLabel = scheduleLabel,
                                participantsLabel = participantsLabel,
                            )
                        }
                    }.onFailure { error ->
                        _state.update { it.copy(isLoading = false, error = error.message) }
                    }
            }
        }
    }

    fun isCurrentUser(principalId: String): Boolean = principalId == sessionManager.userPrincipal

    fun initShowResultOverlay(showResult: Boolean) {
        // Only set to true if it hasn't been dismissed yet
        // This prevents the overlay from reappearing after navigation
        if (showResult && !_state.value.showResultOverlay && _state.value.leaderboard.isEmpty()) {
            _state.update { it.copy(showResultOverlay = true) }
        }
    }

    fun dismissResultOverlay() {
        _state.update { it.copy(showResultOverlay = false) }
    }

    fun onUserClick(row: LeaderboardRow) {
        if (_state.value.isNavigating) return // Prevent multiple clicks

        if (isCurrentUser(row.principalId)) {
            val canisterData =
                CanisterData(
                    canisterId = sessionManager.canisterID ?: getUserInfoServiceCanister(),
                    userPrincipalId = row.principalId,
                    profilePic = sessionManager.profilePic ?: propicFromPrincipal(row.principalId),
                    username = sessionManager.username,
                    isCreatedFromServiceCanister = sessionManager.isCreatedFromServiceCanister ?: false,
                    isFollowing = false,
                )
            viewModelScope.launch { eventChannel.send(Event.OpenProfile(canisterData)) }
            return
        }

        viewModelScope.launch {
            sessionManager.userPrincipal?.let { principal ->
                _state.update { it.copy(isNavigating = true) }
                getProfileDetailsV4UseCase(
                    parameter =
                        GetProfileDetailsV4Params(
                            principal = principal,
                            targetPrincipal = row.principalId,
                        ),
                ).onSuccess { profileDetails ->
                    val canisterData =
                        CanisterData(
                            canisterId = getUserInfoServiceCanister(),
                            userPrincipalId = row.principalId,
                            profilePic = profileDetails.profilePictureUrl ?: propicFromPrincipal(row.principalId),
                            username = row.username,
                            isCreatedFromServiceCanister = true,
                            isFollowing = profileDetails.callerFollowsUser ?: false,
                        )
                    viewModelScope.launch { eventChannel.send(Event.OpenProfile(canisterData)) }
                    _state.update { it.copy(isNavigating = false) }
                }.onFailure { error ->
                    Logger.e("TournamentLeaderboardNavigation") { "Error fetching profile details: $error" }
                    _state.update { it.copy(isNavigating = false) }
                }
            }
        }
    }

    // Telemetry tracking methods
    fun trackResultScreenViewed(
        tournamentId: String,
        isWin: Boolean,
        finalScore: Int,
        rank: Int,
    ) {
        telemetry.onResultScreenViewed(
            tournamentId = tournamentId,
            isWin = isWin,
            finalScore = finalScore,
            rank = rank,
        )
    }

    sealed class Event {
        data class OpenProfile(
            val canisterData: CanisterData,
        ) : Event()
    }
}
