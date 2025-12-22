package com.yral.shared.features.tournament.viewmodel

import com.yral.shared.features.tournament.domain.model.Tournament
import com.yral.shared.features.tournament.domain.model.TournamentError

data class TournamentUiState(
    val selectedTab: Tab = Tab.All,
    val tournaments: List<Tournament> = emptyList(),
    val prizeBreakdownTournament: Tournament? = null,
    val isLoggedIn: Boolean = false,
    val showHowToPlayTournament: Tournament? = null,
    val isLoading: Boolean = false,
    val error: TournamentError? = null,
    val isRegistering: Boolean = false,
    val registrationError: TournamentError? = null,
) {
    enum class Tab { All, History }
}
