package com.yral.shared.features.tournament.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.shared.core.session.SessionManager
import com.yral.shared.features.tournament.domain.GetMyTournamentsUseCase
import com.yral.shared.features.tournament.domain.GetTournamentsUseCase
import com.yral.shared.features.tournament.domain.RegisterForTournamentUseCase
import com.yral.shared.features.tournament.domain.model.GetMyTournamentsRequest
import com.yral.shared.features.tournament.domain.model.GetTournamentsRequest
import com.yral.shared.features.tournament.domain.model.RegisterForTournamentRequest
import com.yral.shared.features.tournament.domain.model.Tournament
import com.yral.shared.features.tournament.domain.model.toUiTournament
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TournamentViewModel(
    private val sessionManager: SessionManager,
    private val getTournamentsUseCase: GetTournamentsUseCase,
    private val getMyTournamentsUseCase: GetMyTournamentsUseCase,
    private val registerForTournamentUseCase: RegisterForTournamentUseCase,
) : ViewModel() {
    private val _state: MutableStateFlow<TournamentUiState> = MutableStateFlow(TournamentUiState())
    val state: StateFlow<TournamentUiState> = _state

    private val eventChannel = Channel<Event>(Channel.BUFFERED)
    val eventsFlow: Flow<Event> = eventChannel.receiveAsFlow()

    init {
        observeSessionState()
        loadTournaments()
    }

    private fun observeSessionState() {
        viewModelScope.launch {
            sessionManager
                .observeSessionPropertyWithDefault(
                    selector = { it.isSocialSignIn },
                    defaultValue = false,
                ).collect { isSocialSignIn ->
                    _state.update { it.copy(isLoggedIn = isSocialSignIn) }
                }
        }
    }

    fun loadTournaments() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            when (_state.value.selectedTab) {
                TournamentUiState.Tab.All -> loadAllTournaments()
                TournamentUiState.Tab.History -> loadMyTournaments()
            }
        }
    }

    private suspend fun loadAllTournaments() {
        getTournamentsUseCase
            .invoke(GetTournamentsRequest())
            .onSuccess { tournamentDataList ->
                val tournaments = tournamentDataList.map { it.toUiTournament() }
                _state.update {
                    it.copy(
                        tournaments = tournaments,
                        isLoading = false,
                        error = null,
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        tournaments = emptyList(),
                        isLoading = false,
                        error = error,
                    )
                }
            }
    }

    private suspend fun loadMyTournaments() {
        val principalId = sessionManager.userPrincipal
        if (principalId.isNullOrEmpty()) {
            _state.update {
                it.copy(
                    tournaments = emptyList(),
                    isLoading = false,
                )
            }
            return
        }

        getMyTournamentsUseCase
            .invoke(GetMyTournamentsRequest(principalId))
            .onSuccess { tournamentDataList ->
                val tournaments = tournamentDataList.map { it.toUiTournament(isRegistered = true) }
                _state.update {
                    it.copy(
                        tournaments = tournaments,
                        isLoading = false,
                        error = null,
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        tournaments = emptyList(),
                        isLoading = false,
                        error = error,
                    )
                }
            }
    }

    fun onTabSelected(tab: TournamentUiState.Tab) {
        _state.update { it.copy(selectedTab = tab) }
        loadTournaments()
    }

    fun registerForTournament(tournament: Tournament) {
        val principalId = sessionManager.userPrincipal
        if (principalId.isNullOrEmpty()) {
            send(Event.Login)
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isRegistering = true, registrationError = null) }

            registerForTournamentUseCase
                .invoke(
                    RegisterForTournamentRequest(
                        tournamentId = tournament.id,
                        principalId = principalId,
                    ),
                ).onSuccess { result ->
                    _state.update { it.copy(isRegistering = false) }
                    send(Event.RegistrationSuccess(result.tournamentId, result.coinsPaid))
                    // Refresh tournaments to update registration state
                    loadTournaments()
                }.onFailure { error ->
                    _state.update {
                        it.copy(
                            isRegistering = false,
                            registrationError = error,
                        )
                    }
                    send(Event.RegistrationFailed(error.message))
                }
        }
    }

    fun openPrizeBreakdown(tournament: Tournament) {
        _state.update { it.copy(prizeBreakdownTournament = tournament) }
    }

    fun closePrizeBreakdown() {
        _state.update { it.copy(prizeBreakdownTournament = null) }
    }

    @Suppress("UnusedParameter")
    fun onShareClicked(tournament: Tournament) {
        // Share functionality will be implemented with platform-specific sharing
    }

    fun onTournamentCtaClick(tournament: Tournament) {
        if (!_state.value.isLoggedIn) {
            send(Event.Login)
            return
        }
        // Navigate to tournament details or start playing
        send(Event.NavigateToTournament(tournament.id))
    }

    fun onNoHistoryCtaClicked() {
        if (state.value.isLoggedIn) {
            onTabSelected(TournamentUiState.Tab.All)
        } else {
            send(Event.Login)
        }
    }

    fun onStartPlayingClicked() {
        _state.update { it.copy(showHowToPlayTournament = null) }
    }

    fun clearError() {
        _state.update { it.copy(error = null, registrationError = null) }
    }

    private fun send(event: Event) {
        viewModelScope.launch { eventChannel.send(event) }
    }

    sealed class Event {
        data object Login : Event()
        data class RegistrationSuccess(
            val tournamentId: String,
            val coinsPaid: Int,
        ) : Event()
        data class RegistrationFailed(
            val message: String,
        ) : Event()
        data class NavigateToTournament(
            val tournamentId: String,
        ) : Event()
    }
}
