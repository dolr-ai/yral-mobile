package com.yral.shared.features.tournament.viewmodel

import androidx.lifecycle.ViewModel
import com.yral.shared.features.tournament.domain.model.Tournament
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class TournamentViewModel : ViewModel() {
    private val _state: MutableStateFlow<TournamentUiState> =
        MutableStateFlow(
            TournamentUiState(
                tournaments = sampleAllTournaments(),
            ),
        )
    val state: StateFlow<TournamentUiState> = _state

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
}
