package com.yral.shared.features.tournament.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yral.shared.core.session.SessionManager
import com.yral.shared.features.tournament.domain.model.Tournament
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TournamentViewModel(
    private val sessionManager: SessionManager,
) : ViewModel() {
    private val _state: MutableStateFlow<TournamentUiState> =
        MutableStateFlow(
            TournamentUiState(
                tournaments = sampleAllTournaments(),
            ),
        )
    val state: StateFlow<TournamentUiState> = _state

    private val eventChannel = Channel<Event>(Channel.BUFFERED)
    val eventsFlow: Flow<Event> = eventChannel.receiveAsFlow()

    init {
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

    fun onTabSelected(tab: TournamentUiState.Tab) {
        _state.update {
            val tournaments =
                when (tab) {
                    TournamentUiState.Tab.All -> sampleAllTournaments()
                    TournamentUiState.Tab.History -> sampleHistoryTournaments()
                }
            it.copy(selectedTab = tab, tournaments = tournaments)
        }
    }

    fun openPrizeBreakdown(tournament: Tournament) {
        _state.update { it.copy(prizeBreakdownTournament = tournament) }
    }

    fun closePrizeBreakdown() {
        _state.update { it.copy(prizeBreakdownTournament = null) }
    }

    @Suppress("EmptyFunctionBlock", "UnusedParameter")
    fun onShareClicked(tournament: Tournament) {
    }

    @Suppress("EmptyFunctionBlock", "UnusedParameter")
    fun onTournamentCtaClick(tournament: Tournament) {
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

    private fun send(event: Event) {
        viewModelScope.launch { eventChannel.send(event) }
    }

    sealed class Event {
        data object Login : Event()
    }
}
